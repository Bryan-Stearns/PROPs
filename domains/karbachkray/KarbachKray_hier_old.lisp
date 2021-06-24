;;; Task Switching, Working memory capacity and Stroop model
;;; 
;;; Copyright 2012 Niels Taatgen, modified by Bryan Stearns 2020 for PROP3
;;;
;;; Model of the Karbach and Kray (2009) experiment
;;;


;;; Instructions for the count span task


(add-instr count-span :input (Vobject Vcolor Vshape) :variables (WMcount WMprev)  :declarative ((RTcount RTfirst RTsecond)(RTcount RTprev))
:pm-function count-span-action
:init init-count-span
:reward 10.0 ;; might take long
:parameters ((sgp :lf 0.05 :egs 0.3 :ans 0.1 :rt -0.7 :perception-activation 0.0 :imaginal-activation 1.0 :mas nil :alpha 0.03)(setf *condition-spread* 1.0)   
             (spp retrieve-instruction :u 2.0)
          (setf *verbose* nil)
)  ;; :ans was 0.2
	
	;~ init
	(ins :condition (Gtop = nil) :action (WMid -> Gtop (wait) -> AC) :description "Set count to zero and wait for first screen")
	;~ do-task
	(ins :condition (Gtop <> nil) :subgoal "cs-read-prompt" :description "Once initialized, enter the problem space of reacting to prompts")
)


(add-instr cs-do-task :input (Vobject Vcolor Vshape) :variables (WMcount WMprev)  :declarative ((RTcount RTfirst RTsecond)(RTcount RTprev))
:pm-function count-span-action
:init init-count-span
:reward 10.0 ;; might take long
	;~ idle
	(ins :condition (Vobject = pending) :subgoal "idle" :description "If there is nothing to see, just wait")
	;~ prepare
	(ins :condition (Vobject = pending WMCount <> nil) :subgoal "prepare" :description "If there is nothing to see, start rehearsing!")

	;~ count-circles
	(ins :condition (Vobject = yes) :subgoal "count-circles" :description "If a new screen is up, start a new count")	;; Is this going to blink as new countable items are attended to?
	;~ store-count
	(ins :condition (Vobject = last) :action ((nil WMid) -> newWM (say WMcount) -> AC) :description "No more object, remember count")
	;~ cs-report
	(ins :condition (Vobject = report) :subgoal "cs-report" :description "Report is asked for, start reporting")

)

(add-instr count-circles :input (Vobject Vcolor Vshape) :variables (WMcount WMprev)  :declarative ((RTcount RTfirst RTsecond)(RTcount RTprev))
:pm-function count-span-action
:init init-count-span
:reward 10.0 ;; might take long
	;~ ignore-color
	(ins :condition (Vcolor <> blue) :action ((attend-next) -> AC) :description "Green thing, so ignore")
	;~ ignore-shape
	(ins :condition (Vshape <> circle) :action ((attend-next) -> AC) :description "Square, so ignore")
	;~ see-blue-circle
	(ins :condition (Vcolor = blue  Vshape = circle  RTsecond = nil) :action ( (count-fact WMcount) -> RT) :description "Blue circle, retrieve next count")
	;~ update-count
	(ins :condition (RTsecond <> nil) :action (RTsecond -> WMcount (attend-next) -> AC) :description "Update count, attend next object")
)

(add-instr cs-rehearse :input (Vobject Vcolor Vshape) :variables (WMcount WMprev)  :declarative ((RTcount RTfirst RTsecond)(RTcount RTprev))
:pm-function count-span-action
:init init-count-span
:reward 10.0 ;; might take long
	;~ start
	(ins :condition (RT1 = nil) :action (Gtop -> RTid) :description "Start rehearsing")  ;; (suppress-pending) -> AC
	;~ next
	(ins :condition (RT1 <> error) :action (RTid -> RTprev rehearse -> Gcontrol) :description "Rehearse the next item")
	;~ restart
	(ins :condition (RT1 = error) :action (Gtop -> RTid) :description "If we're done with the list, start again!")
)

(add-instr cs-report :input (Vobject Vcolor Vshape) :variables (WMcount WMprev)  :declarative ((RTcount RTfirst RTsecond)(RTcount RTprev))
:pm-function count-span-action
:init init-count-span
:reward 10.0 ;; might take long
	;~ start
	(ins :condition (RT1 = nil) :action (Gtop -> RTid) :description "Start reporting")
	;~ report
	(ins :condition (RT1 <> error) :action (RTid -> RTprev (report RTcount) -> AC) :description "Report item and retrieve next")
	;~ finish
	(ins :condition (RT1 = error) :action (finish -> Gtask) :description "No more items: done")
)


;;; What is the sequence?
;;; Initial: PS empty, V empty
;;; Experimental code puts something in V1: V1 has an object
;;; First instruction puts something in PS1: PS1 has the task
;;; Second instruction gets the property of the object in V1: PS1 task, V1 is empty again, V2 has the property
;;; Retrieval gets the key corresponding to V2: PS1 still has the task, V1 is empty V2 has the property and V3 has the key
;;; After key has been pressed, PS1 has the task, and V1-V3 should be empty (action should clear them)

(add-instr single-task-A :input (Vobject Vfood Vsize)  :declarative ((RTmapping RTcategory RTkey))
:pm-function single-task-AB
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 mapping slot2 vegetable slot3 "s")(isa fact slot1 mapping slot2 fruit slot3 "k"))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)
;;; Initialize the first trial
	(ins :condition (Vobject = pending) :action ((wait) -> AC) :description "Wait for next stimulus")
	(ins :condition (Vobject = yes) :action ((get-property food-property) -> AC) :description "Stimulus detected, get food-property")
	(ins :condition (Vfood <> nil RTkey = nil) :action ((mapping Vfood) -> RT) :description "Retrieve key related to food concept")
	(ins :condition (RTkey <> nil) :action ((press-key RTkey) -> AC) :description "Press appropriate key")
	(ins :condition (Vobject = last) :action (finish -> Gtask) :description "Done with the block")

)

(add-instr single-task-B :input (Vobject Vfood Vsize)  :declarative ((RTmapping RTcategory RTkey))
:pm-function single-task-AB
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 mapping slot2 small slot3 "s")(isa fact slot1 mapping slot2 large slot3 "k"))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)
;;; Initialize the first trial
	(ins :condition (Vobject = pending) :action ((wait) -> AC))
	(ins :condition (Vobject = yes) :action ((get-property size-property) -> AC))
	(ins :condition (Vsize <> nil RTkey = nil) :action ((mapping Vsize) -> RT))
	(ins :condition (RTkey <> nil) :action ((press-key RTkey) -> AC))
	(ins :condition (Vobject = last) :action (finish -> Gtask))

;;; Also: the simulation now has to wait for the next stimulus
)

(add-instr task-switching-AB :input (Vobject Vfood Vsize) :variables (WMcur-task WMcount) :declarative ((RTmapping RTcategory RTkey)(RTother RTfirst RTsecond))
:pm-function single-task-AB
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 other-task slot2 food-task slot3 size-task)(isa fact slot1 other-task slot2 size-task slot3 food-task))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)

	(ins :condition (WMcur-task = nil) :action (food-task -> WMcur-task  one -> WMcount do-task -> Gcontrol (wait) -> AC) :description "Initialize task, set task to food, wait for first stimulus")
	(ins :condition (Gcontrol = do-task Vobject = yes WMcur-task = food-task ) :action ((get-property food-property) -> AC) :description "Task is food so get food property")
	(ins :condition (Gcontrol = do-task Vobject = yes WMcur-task = size-task) :action  ((get-property size-property) -> AC) :description "Task is size so get size property")
	(ins :condition (Vfood <> nil RTkey = nil) :action ((mapping Vfood) -> RT) :description "Get food - key mapping")
	(ins :condition (Vsize <> nil RTkey = nil) :action ((mapping Vsize) -> RT) :description "Get Size - key mapping")
	(ins :condition (Gcontrol = do-task RTkey <> nil) :action ((press-key RTkey) -> AC) :description "Press appropriate key")
	(ins :condition (Vobject = pending  Gcontrol = do-task) :action (choose-task -> Gcontrol) :description "While waiting determine next task")
	(ins :condition (Vobject = pending  Gcontrol <> choose-task) :action (choose-task -> Gcontrol (wait) -> AC) :description "Not preparing but waiting")
	(ins :condition (WMcount = one Gcontrol = choose-task) :action (do-task -> Gcontrol two -> WMcount (wait) -> AC) :description "If we have done the task once increase count and wait")
	(ins :condition (RTsecond = nil WMcount = two Gcontrol = choose-task ) :action ((other-task WMcur-task) -> RT) :description "If we have done it twice retrieve the other task")
	(ins :condition (RTsecond <> nil Gcontrol = choose-task ) :action (do-task -> Gcontrol one -> WMcount RTsecond -> WMcur-task (wait) -> AC) :description "Set the task to the retrieved task, counter to one, and wait")
	(ins :condition (Vobject = last) :action (finish -> Gtask) :description "Done with the block")
)


;;; Now tasks C and D.

(add-instr single-task-C :input (Vobject Vtransport Vnumber)  :declarative ((RTmapping RTcategory RTkey))
:pm-function single-task-CD
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 mapping slot2 plane slot3 "s")(isa fact slot1 mapping slot2 car slot3 "k"))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)
;;; Initialize the first trial
	(ins :condition (Vobject = pending) :action ((wait) -> AC))
	(ins :condition (Vobject = yes) :action ((get-property transport-property) -> AC))
	(ins :condition (Vtransport <> nil RTkey = nil) :action ((mapping Vtransport) -> RT))
	(ins :condition (RTkey <> nil) :action ((press-key RTkey) -> AC))
	(ins :condition (Vobject = last) :action (finish -> Gtask))

)


(add-instr single-task-D :input (Vobject Vtransport Vnumber)  :declarative ((RTmapping RTcategory RTkey))
:pm-function single-task-CD
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 mapping slot2 one slot3 "s")(isa fact slot1 mapping slot2 two slot3 "k"))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)
;;; Initialize the first trial
	(ins :condition (Vobject = pending) :action ((wait) -> AC))
	(ins :condition (Vobject = yes) :action ((get-property number-property) -> AC))
	(ins :condition (Vnumber <> nil RTkey = nil) :action ((mapping Vnumber) -> RT))
	(ins :condition (RTkey <> nil) :action ((press-key RTkey) -> AC))
	(ins :condition (Vobject = last) :action (finish -> Gtask))
)

(add-instr task-switching-CD :input (Vobject Vtransport Vnumber) :variables (WMcur-task WMcount) :declarative ((RTmapping RTcategory RTkey)(RTother RTfirst RTsecond))
:pm-function single-task-CD
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 other-task slot2 transport-task slot3 number-task)(isa fact slot1 other-task slot2 number-task slot3 transport-task))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)

	(ins :condition (WMcur-task = nil) :action (transport-task -> WMcur-task  one -> WMcount do-task -> Gcontrol (wait) -> AC))
	(ins :condition (Gcontrol = do-task Vobject = yes WMcur-task = transport-task ) :action ((get-property transport-property) -> AC))
	(ins :condition (Gcontrol = do-task Vobject = yes WMcur-task = number-task) :action  ((get-property number-property) -> AC))
	(ins :condition (Vtransport <> nil RTkey = nil) :action ((mapping Vtransport) -> RT))
	(ins :condition (Vnumber <> nil RTkey = nil) :action ((mapping Vnumber) -> RT))
	(ins :condition (Gcontrol = do-task RTkey <> nil) :action ((press-key RTkey) -> AC))
	(ins :condition (Vobject = pending  Gcontrol = do-task) :action (choose-task -> Gcontrol))  ;; 32
	;(ins :condition (Vobject = pending  Gcontrol <> choose-task) :action (choose-task -> Gcontrol (wait) -> AC) :description "Not preparing but waiting")
	(ins :condition (WMcount = one Gcontrol = choose-task) :action (do-task -> Gcontrol two -> WMcount (wait) -> AC))
	(ins :condition (RTsecond = nil WMcount = two Gcontrol = choose-task ) :action ((other-task WMcur-task) -> RT))
	(ins :condition (RTsecond <> nil Gcontrol = choose-task ) :action (do-task -> Gcontrol one -> WMcount RTsecond -> WMcur-task (wait) -> AC))
	(ins :condition (Vobject = last) :action (finish -> Gtask))
)


(add-instr stroop :input (Vobject Vcolor Vword)  :declarative ((RTmapping RTstimulus RTconcept RTstim-type))
:pm-function stroop-pm
:init stroop-init
:reward 13.0
:facts ((red-word-assoc isa fact slot1 s-mapping slot2 red-word slot3 red-concept slot4 word-task)
        (blue-word-assoc isa fact slot1 s-mapping slot2 blue-word slot3 blue-concept slot4 word-task)
        (red-color-assoc isa fact slot1 s-mapping slot2 red-color slot3 red-concept slot4 color-task)
        (blue-color-assoc isa fact slot1 s-mapping slot2 blue-color slot3 blue-concept slot4 color-task)
        (red-color isa chunk)(red-word isa chunk)(blue-color isa chunk)(blue-word isa chunk)(train-word isa chunk))
:parameters ((sgp :lf 0.05 :egs 0.3 :rt -3.0 :perception-activation 2.0 :mas 3.0 :alpha 0.02 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 1.0)
(setf *condition-spread* 1.0) 
             (sdp (red-color-assoc blue-color-assoc red-word-assoc blue-word-assoc) :references 1000 :creation-time -40000000)
             (add-sji (red-word red-word-assoc 1.5)(red-word blue-word-assoc -1.5)
             		  (red-color blue-color-assoc -1.5)(red-color red-color-assoc 1.5)
             		  (blue-word red-word-assoc -1.5)(blue-word blue-word-assoc 1.5)
                          (blue-word red-color-assoc -1.5)(blue-word blue-color-assoc 1.5)
                          (red-word blue-color-assoc -1.5)(red-word red-color-assoc 1.5)
             		  (blue-color blue-color-assoc 1.5)(blue-color red-color-assoc -1.5))
             (setf *verbose* nil)
             
             )

	;~ prepare
	(ins :condition (Vobject != last RTconcept != Vcolor) :subgoal "prepare" :action (prepare -> Gtask prepare -> Gcontrol (wait) -> AC) :description "Prepare while waiting for stimulus")
	;~ idle
	(ins :condition (Vobject != last RTconcept != Vcolor) :subgoal "idle" :action (idle -> Gtask neutral -> Gcontrol (wait) -> AC) :description "Do nothing while waiting for stimulus")

	;~ say-correct
	(ins :condition (RTconcept = Vcolor) :action ((say RTconcept) -> AC) :description "Say the answer")

	;~ finish
	(ins :condition (Vobject = last) :action (finish -> Gtask) :description "Done with this block")

)

(add-instr stroop-answer :input (Vobject Vcolor Vword) :declarative ((RTmapping RTstimulus RTconcept RTstim-type))
:pm-function stroop-pm

	;~ wait
	(ins :condition (Vobject <> yes) :subgoal "idle" :description "Do nothing while waiting for stimulus")

	;~ retrieve-color
	(ins :condition (Vcolor <> nil) :action ((s-mapping Vcolor) -> RT) :description "Retrieve color concept of the ink color")
)

;;; PREPARE : Redirect to another subgoal directly, based on the current task
(add-instr prepare :input (Vobject Videntity) :working-memory (WMconcept WMprev) :declarative ((RTisword RTlexical RTanswer)(RTconcept RTprev))
:pm-function VCWM-action

	;; For VCWM, prepare by rehearsing
	;~ count-span
	(ins :condition (Vtask = count-span) :subgoal "cs-rehearse" :description "Prepare for count-span by rehearsing the list")

	;; When the stroop stimulus arrives, the model can use its preparation to focus on just color, or it can process both properties after all
	;~ stroop
	(ins :condition (Vtask = stroop) :subgoal "stroop-answer" :description "Prepare for stroop by focusing on color")
)

;;; IDLE : Redirect to another subgoal when the stimulus appears, based on the current task
(add-instr idle :input (Vobject Vcolor Vword) :working-memory (WMconcept WMprev) :declarative ((RTmapping RTstimulus RTconcept RTstim-type))

	;~ wait
	(ins :condition (Vobject = pending) :action ((wait) -> AC) :description "Wait until something happens.")

	;;~ retrieve-text
	;(ins :condition (Vtask = stroop Vword = Vcolor) :action ((s-mapping Vword) -> RT) :description "Congruent: retrieve the word")
	;~ focus-stroop-answer
	(ins :condition (Vtask = stroop Vword <> Vcolor) :subgoal "stroop-answer" :description "Incongruent: need to focus")
)
