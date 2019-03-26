;;; Working memory capacity and Stroop model
;;; 
;;; Copyright 2012 Niels Taatgen
;;;
;;; Model of the Chein and Morrison (2010) experiment
;;;

;;; The full experiment can be run by calling (do-chein). Output will be written to StroopChein.txt and WMChein.txt
;;; (test) Initializes the VCWM model for just one run
;;; (do-one 1) runs one full session of VCWM
;;; (test-stroop) will run a block of Stroop


;;; First some code that simulates the Verbal Complex Working Memory task

(defstruct VCWM current-span responses last-correct trials state start count stimuli spans)

(defvar *cstask*)
(defvar *results*)
(defvar *outputdir* "~/")


(defun init-VCWM ()
  (setf *cstask* (make-VCWM))
  (setf (VCWM-state *cstask*) 'lexical)
  (setf (VCWM-current-span *cstask*) 4)
  (setf (VCWM-last-correct *cstask*) 'undef)
  (setf (VCWM-trials *cstask*) 16)
  (setf (VCWM-responses *cstask*) nil)
  (setf (VCWM-stimuli *cstask*) nil)
  (setf (VCWM-count *cstask*) (1- (VCWM-current-span *cstask*)))  ;;; keep a counter
  (setf (VCWM-start *cstask*) (+ (mp-time) 0.365))
  (schedule-delayed-action (list 'word  (nth (random 4) '(umbrella tantrum xobos fartnot))) 0.5)
)


;;; Perceptual motor function implements the following actions:

;;; Type: used for the lexical decision task to enter "Y" or "N" for word or non-word responses, and to enter the letters in the report stage
;;; Enter: indicates that all the letters have been entered in the report stage
;;; Model also uses "Wait", but this is predefined. Wait waits until the next stimulus appears.

(defun VCWM-action (action &optional h1 h2)
  
  (let ((latency 0.05))
    (cond 
     ((and (eq action 'type)(eq (VCWM-state *cstask*) 'lexical)(< (- (mp-time) (VCWM-start *cstask*)) 4.0))
      (schedule-delayed-action (list 'word  (nth (random 4) '(umbrella tantrum xobos fartnot))) 0.5)) 
     ((and (eq action 'type)(eq (VCWM-state *cstask*) 'lexical)) ;;; four seconds are up
      (trigger-reward  (task-reward *task*)) ;; give a reward
       (let ((next-letter (nth (random 10) '(a b c d e f g h i j))))
        (if (zerop (VCWM-count *cstask*)) 
            (progn
             (setf (VCWM-state *cstask*) 'report)
              (schedule-delayed-action '(report) 1.135)) ;;; last letter, schedule a report
          (progn            
            (decf (VCWM-count *cstask*))
            (setf (VCWM-start *cstask*) (+ (mp-time) 1.635))
            (schedule-delayed-action (list 'word  (nth (random 4) '(umbrella tantrum xobos fartnot))) 1.135))) ;;; otherwise, schedule next lexical decision
        (setf *perception* (list 'letter next-letter))
;          (setf (VCWM-state *cstask*) 'letter)
        (push next-letter (VCWM-stimuli *cstask*))
;          (setf (VCWM-start *cstask*) (mp-time))
        (setf latency 0.135)))
     ((and (eq action 'type) (eq (VCWM-state *cstask*) 'report))
           (push h1 (VCWM-responses *cstask*))
           (setf latency .2))
     ((eq action 'enter)  
      (trigger-reward  (task-reward *task*)) ;; give a reward
      (let ((correct (equal (VCWM-responses *cstask*)(VCWM-stimuli *cstask*))))
        (push (list (VCWM-current-span *cstask*) correct) (VCWM-spans *cstask*))
        (when (and (not (eq (VCWM-last-correct *cstask*) 'undef)) (> (VCWM-current-span *cstask*) 1))
          (when (and correct (VCWM-last-correct *cstask*)) (incf (VCWM-current-span *cstask*))(setf correct 'undef))
          (when (and (not correct)(not (VCWM-last-correct *cstask*))) (decf (VCWM-current-span *cstask*))(setf correct 'undef)))
        (setf (VCWM-last-correct *cstask*) correct)
        (setf (VCWM-state *cstask*) 'lexical)
        (setf *perception* nil)
        (decf (VCWM-trials *cstask*))
        ;; done with entering the stimuli
        ;;; handle this later let's test this stuff first
        (format t "~%Enter has been pushed, current span ~D~%" (VCWM-current-span *cstask*)))
      (print *cstask*)
      ;;; Set up the next trial
      (when (> (VCWM-trials *cstask*) 0)
            (setf (VCWM-responses *cstask*) nil)
            (setf (VCWM-stimuli *cstask*) nil)
            (setf (VCWM-count *cstask*) (1- (VCWM-current-span *cstask*)))  ;;; keep a counter
            (setf (VCWM-start *cstask*) (+ (mp-time) 0.365))
            (schedule-delayed-action (list 'word  (nth (random 4) '(umbrella tantrum xobos fartnot))) 0.5))
      
      ))
    latency)
)
           
(defun test () (set-task 'verbal-CWM) (init-task) (sgp :v nil :save-buffer-trace t)(setf *verbose* 'full))       

(defun do-one (n)
  (set-task 'verbal-CWM)
  (setf *results* nil)
  (dotimes (i n)
    (format t "~%*** Session ~D ***~%" (1+ i))
    (init-task)
    (sgp :v nil :save-buffer-trace nil)
    (sgp :alpha 0.2)
    (dotimes (i 16)(run 100))
    (push (reverse (vcwm-spans *cstask*)) *results*)
)
  (print (reverse *results*))
)


;;; Run code for the Stroop task
;;;

(defstruct strooptask task-id starttime count numtrials type answer)

(defvar *sttask*)

(setf *vertices* nil)
(setf *edges* nil)

;; Make sure the number of trials is such that it takes approximately as long as a CWM trial, which is approximately 35 seconds. Let's say roughly 15 Stroop trials
(defun stroop-init ()
  (setf *sttask* (make-strooptask))
  (setf (strooptask-count *sttask*) 0)
  (setf (strooptask-numtrials *sttask*) 15) ;;; default
  (setf *results* nil)
  (setf (strooptask-starttime *sttask*) (+ (mp-time) 1.0)) ;;; start at t=1
  (schedule-delayed-action '(yes) 1.0) ;; Schedule first stimulus
  )


;;; The Stroop actions are the following:
;;; get-property. Can be called with color-property, in which case it only gives the color of the ink, otherwise it gives both word and color
;;; say. Say the answer

(defun stroop-pm (action &optional h1 h2)
	(let ((latency 0.05))
	(case action
	  (get-property 
           (if (= (mod (strooptask-count *sttask*) 2) 0)
               (setf (strooptask-type *sttask*) 'congruent
                     (strooptask-answer *sttask*) 'red-concept
                     *perception* '(rred red-color red-word))
             (setf (strooptask-type *sttask*) 'incongruent
                   (strooptask-answer *sttask*) 'red-concept
                   *perception* '(rblue red-color blue-word)))
           (when (eq h1 'color-property) (setf *perception* (list 'rblue (second *perception*))))  ;;; If we ask for color specifically leave out the distractor
           (setf latency 0.2)) 
	  (say (setf latency 0.2)
              (let ((correct (equal h1 (strooptask-answer *sttask*))))
                (trigger-reward (if correct (task-reward *task*) 0))
                (push (list (1+ (strooptask-count *sttask*))(+ (mp-time) 0.2 (- (strooptask-starttime *sttask*))) (strooptask-type *sttask*) correct) *results*))
	    (let ((new-percept (if (> (incf (strooptask-count *sttask*)) (strooptask-numtrials *sttask*)) '(last)
                                 (progn (setf (strooptask-starttime *sttask*) (+ (mp-time) 1.2))
                                   '(yes)))))
              (schedule-delayed-action new-percept 1.2)

;	    (format t "~%*** Answer ~A for trial ~A at time ~6,3F~%" h1 (strooptask-type *sttask*) (+ (mp-time) 0.2 (- (strooptask-starttime *sttask*))))
	    )))

     latency))


(defun do-stroop (&optional (day 1)(condition 'NONE)(nolearn nil))  (set-task 'stroop)
  (set-task 'stroop)
  (init-task)
  ;;; Practice
  (sgp :v nil :save-buffer-trace nil)
  (dotimes (i 12)  ;;; real experiment has 3 blocks of 60, but we do 12 blocks of 15 to make them as long as the CWM trials
     (init-task)
     (when nolearn (sgp :alpha 0))
     (run 10000)
     (with-open-file (f (concatenate 'string *outputdir* "stroopChein.txt") :direction :output :if-exists :append :if-does-not-exist :create)
       (dolist (x (reverse *results*))
         (format t "~%STROOP ~A ~D ~D ~D ~A ~D ~6,3F" condition (1+ i) day (first x) (third x)(if (fourth x) 1 0)(second x))
         (format f "STROOP ~A ~D ~D ~D ~A ~D ~6,3F~%" condition (1+ i) day (first x) (third x)(if (fourth x) 1 0)(second x))
         ))))

(defun test-stroop ()
  (set-task 'stroop)
  (setf *verbose* 'full)
  (init-task)
   (setf (strooptask-numtrials *sttask*) 4) ;;; default
  ;;; Practice
  (sgp :v nil :save-buffer-trace t) (setf *verbose* 'full)
  (run 10000))


(defun means ()
  (dolist (y
  (reverse (mapcar #'(lambda (x) (/ (apply #'+ x) (length x))) *results*)))
    (format t "~%~6,3F" y)))

(defun do-chein ()
  (reset)
  (do-stroop 1 'CONTROL)  ;; put back later
  (reset)
 (do-stroop 21 'CONTROL)
  (reset)
  (do-stroop 1 'EXP)
  (reset)
   (set-task 'verbal-CWM)

   (dotimes (i 2) (format t  "~%*** Practice Session ~D ***~%" (1+ i))
    (init-task)
    (sgp :v nil :save-buffer-trace nil)
 
    (dotimes (j 16) (run 200)))
   (setf *results* nil)    
   (dotimes (i 20)
     (format t "~%*** Session ~D ***~%" (1+ i))
     (init-task)
     (sgp :v nil :save-buffer-trace nil)

     (dotimes (j 16) (run 200))
     (push (reverse (vcwm-spans *cstask*)) *results*)
   (with-open-file (f (concatenate 'string *outputdir* "WMChein.txt") :direction :output :if-exists :append :if-does-not-exist :create)
     (dolist (x (first *results*))
        (format f "~D  ~D ~D ~%"  (1+ i) (first x)(if (second x) 1 0))
      
       (format t "~%~D  ~D ~D "  (1+ i) (first x)(if (second x) 1 0))))
   )
  (print (reverse *results*))
  (do-stroop 21 'EXP)

)

(defun run-experiment (n)
  (dotimes (i n) (do-chein)))

(defun run-sample (&optional x)
  (case x
    (1 (print "Running Stroop") (test-stroop))
    (2 (print "Running Verbal CWM") (test)(run 100))

    (otherwise (print "1 - Stroop  2 - Verbal CWM"))))


;;; In order to run this uncomment the simple model first

(clear-all)

(define-model-transfer


(add-instr verbal-CWM :input (Vobject Videntity) :working-memory (WMconcept WMprev) :declarative ((RTisword RTlexical RTanswer)(RTconcept RTprev))
:pm-function VCWM-action
:init init-VCWM
:reward 10.0 
:facts ((isa fact slot1 is-word slot2 tantrum slot3 yes)(isa fact slot1 is-word slot2 umbrella slot3 yes))  ; for lexical decision
:parameters ((sgp :lf 0.1 :egs 0.3 :ans 0.1 :rt -0.6 :perception-activation 0.0  :mas nil :alpha 0.02)(setf *condition-spread* 1.0) (spp retrieve-instruction :u 2.0)             (setf *verbose* nil))  ;; :rt was 0.0, :lf was 0.05

;; Initialize the pointer into declarative memory by pointing it at working memory. This will become the first memory item
;~ init
(ins :condition (Gtop = nil) :action (WMid -> Gtop) :description "Start with lexical decision")  ;; Test Gtop also so that action is add instead of replace


;; Do lexical decision
(ins :condition (Vobject = word RTisword != is-word RT1 != error) :subgoal "VCWM-lexdec" :description "Word is seen, respond to it.")

;~ success
(ins :condition (RTanswer = yes RTisword = is-word) :action (nil -> Gcontrol (type Y) -> AC) :description "Successful retrieve: respond 'yes'")
;~ fail
(ins :condition (RT1 = error) :action (nil -> Gcontrol (type N) -> AC) :description "Retrieval failure: respond 'no'")

;;~ return
;(ins :condition (Vobject <> word) :action (Gparent -> Gtask) :description "Return to the parent task if the word disappears before we're ready.")

;; When a letter appears, put it in WM and prepare to rehearse
;~ store-wm
(ins :condition (Vobject = letter WMconcept = nil) :action (Videntity -> WMconcept (wait) -> AC) :description "A letter is presented: put it in WM")
;~ store-dm
(ins :condition (Vobject = letter WMconcept <> nil) :action ((Videntity WMid) -> newWM (wait) -> AC) :description "A letter is presented: put it in DM")


;~ prepare
(ins :condition (Vobject = pending WMconcept <> nil) :subgoal "prepare" :description "Start rehearsing")
;~ report
(ins :condition (Vobject = report) :subgoal "VCWM-report" :description "Start reporting")

;;~ wait
;(ins :condition (Vobject = pending) :subgoal "idle" :action (Gtask -> Gparent idle -> Gtask) :description "Start idling")

)


(add-instr VCWM-rehearse :input (Vobject Videntity) :working-memory (WMconcept WMprev) :declarative ((RTisword RTlexical RTanswer)(RTconcept RTprev))
:pm-function VCWM-action

;; Do rehearsal as long as the letter is still on the screen
;~ start
(ins :condition (RT1 = nil) :action (Gtop -> RTid) :description "Start rehearsal by retrieving first item")
;~ next
(ins :condition (RT1 <> error) :action ((? RTid) -> RT) :description "Rehearse next item") ;; '!=' for inexistence test, so when RT1 blinks this operator will also blink
;~ restart
(ins :condition (RT1 = error) :action (Gtop -> RTid) :description "End of list, return to top")

;;~ return
;(ins :condition (Vobject <> pending) :action (Gparent -> Gtask) :description "Interupted, return to task.")
)

(add-instr VCWM-lexdec :input (Vobject Videntity) :working-memory (WMconcept WMprev) :declarative ((RTisword RTlexical RTanswer)(RTconcept RTprev))
:pm-function VCWM-action

;~ retrieve
(ins :condition (Vobject = word) :action ((is-word Videntity) -> RT) :description "Retrieve the word")
)

(add-instr prepare :input (Vobject Videntity) :working-memory (WMconcept WMprev) :declarative ((RTisword RTlexical RTanswer)(RTconcept RTprev))
:pm-function VCWM-action

;; For VCWM, prepare by rehearsing
;~ VCWM
(ins :condition (Vtask = verbal-CWM) :subgoal "VCWM-rehearse" :description "Prepare for VCWM by rehearsing the list")

;; When the stroop stimulus arrives, the model can use its preparation to focus on just color, or it can process both properties after all
;~ stroop
(ins :condition (Vtask = stroop) :subgoal "stroop-answer" :description "Prepare for stroop by focusing on color")
)


(add-instr idle :input (Vobject Vcolor Vword) :working-memory (WMconcept WMprev) :declarative ((RTmapping RTstimulus RTconcept RTstim-type))

;~ wait
(ins :condition (Vobject = pending) :action ((wait) -> AC) :description "Wait until something happens.")

;~ retrieve-text
(ins :condition (Vtask = stroop Vword = Vcolor) :action ((s-mapping Vword) -> RT) :description "Congruent: retrieve the word")
;~ focus-stroop-answer
(ins :condition (Vtask = stroop Vword <> Vcolor) :subgoal "stroop-answer" :description "Incongruent: need to focus")

)


(add-instr VCWM-report :input (Vobject Videntity) :working-memory (WMconcept WMprev WMvar) :declarative ((RTisword RTlexical RTanswer)(RTconcept RTprev))
:pm-function VCWM-action
;; Do the report
;;~ report
;~ start
(ins :condition (WMvar != report) :action (Gtop -> RTid report -> WMvar) :description "Report prompt came up: retrieve first item")
;~ report
(ins :condition (RT1 <> error WMvar = report) :action ((type RTconcept) -> AC (? RTid) -> RT) :description "Report item and retrieve next")
;~ finish
(ins :condition (RT1 = error WMvar = report) :action ((enter) -> AC finish -> Gtask) :description "No more items: press enter and end")


)



(add-instr stroop :input (Vobject Vcolor Vword) :variables (WMconcept) :declarative ((RTmapping RTstimulus RTconcept RTstim-type))
:pm-function stroop-pm
:init stroop-init
:reward 13.0
:facts ((red-word-assoc isa fact slot1 s-mapping slot2 red-word slot3 red-concept slot4 word-task)
        (blue-word-assoc isa fact slot1 s-mapping slot2 blue-word slot3 blue-concept slot4 word-task)
        (red-color-assoc isa fact slot1 s-mapping slot2 red-color slot3 red-concept slot4 color-task)
        (blue-color-assoc isa fact slot1 s-mapping slot2 blue-color slot3 blue-concept slot4 color-task)
;        (train-word-assoc isa fact slot1 s-mapping slot2 train-word slot3 train-concept slot4 word-task)
        (red-color isa chunk)(red-word isa chunk)(blue-color isa chunk)(blue-word isa chunk)(train-word isa chunk))
:parameters ((sgp :lf 0.1 :egs 0.3 :rt -3.0 :perception-activation 2.0 :mas 3.0 :alpha 0.02)(setf *condition-spread* 1.0) (spp retrieve-instruction :u 1.0) 
             (sdp (red-color-assoc blue-color-assoc red-word-assoc blue-word-assoc train-word-assoc) :references 1000 :creation-time -40000000)
             (add-sji (red-word red-word-assoc 1.5)(red-word blue-word-assoc -1.5)
             		  (red-color blue-color-assoc -1.5)(red-color red-color-assoc 1.5)
             		  (blue-word red-word-assoc -1.5)(blue-word blue-word-assoc 1.5)
                          (blue-word red-color-assoc -1.5)(blue-word blue-color-assoc 1.5)
                          (red-word blue-color-assoc -1.5)(red-word red-color-assoc 1.5)
             		  (blue-color blue-color-assoc 1.5)(blue-color red-color-assoc -1.5))
             (setf *verbose* nil)
             
             )

;;~ init
;(ins :condition (Gcontrol = nil) :action (neutral -> Gcontrol (wait) -> AC) :description "Start Stroop task, wait for stimulus")

;~ prepare
(ins :condition (Vobject != last RTconcept != Vcolor) :subgoal "prepare" :action (prepare -> Gtask prepare -> Gcontrol (wait) -> AC) :description "Prepare while waiting for stimulus")
;~ idle
(ins :condition (Vobject != last RTconcept != Vcolor) :subgoal "idle" :action (idle -> Gtask neutral -> Gcontrol (wait) -> AC) :description "Do nothing while waiting for stimulus")

;~ say-correct
(ins :condition (RTconcept = Vcolor) :action ((say RTconcept) -> AC) :description "Say the answer")

;~ finish
(ins :condition (Vobject = last) :action (finish -> Gtask) :description "Done with this block")

)

;(add-instr stroop-recog :input (Vobject Vcolor Vword) :variables (WMconcept) :declarative ((RTmapping RTstimulus RTconcept RTstim-type))
;:pm-function stroop-pm
;;; Recognize that the percepts are incongruent and figure out how to respond

;;~ focus-stroop-answer
;(ins :condition (Vtask = stroop Vword <> Vcolor) :subgoal "stroop-answer" :description "Incongruent: need to focus")

;)


(add-instr stroop-answer :input (Vobject Vcolor Vword) :variables (WMconcept) :declarative ((RTmapping RTstimulus RTconcept RTstim-type))
:pm-function stroop-pm

;; If the color is perceived, retrieve the color concept related to the color. This process is slowed down if there is a conflicting word in Vword. After the concept is retrieved, say it.
;;~ focus
;(ins :condition (Vobject = yes Vcolor = nil) :action ((get-property both) -> AC) :description "Object seen, focus on just color") 

;~ wait
(ins :condition (Vobject <> yes) :subgoal "idle") :description "Do nothing while waiting for stimulus")

;~ retrieve-color
(ins :condition (Vcolor <> nil) :action ((s-mapping Vcolor) -> RT) :description "Retrieve color concept of the ink color")

)


(sdp :reference-count 3000 :creation-time -1000000)


)

