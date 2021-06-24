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
	(ins :condition (Gtop = nil) :action (WMid -> Gtop zero -> WMcount (begin) -> AC) :description "Set count to zero and wait for first screen")
	;~ read-prompt
	(ins :condition (Gtop <> nil) :subgoal "cs-read-prompt" :description "Once initialized, enter the problem space of reacting to task prompts")
)


(add-instr cs-read-prompt :input (Vobject Vcolor Vshape) :variables (WMcount WMprev)  :declarative ((RTcount RTfirst RTsecond)(RTcount RTprev))
:pm-function count-span-action
:init init-count-span
:reward 10.0 ;; might take long
	;~ idle
	(ins :condition (Vobject = pending) :subgoal "idle" :description "If there is nothing to see, just wait")
	;~ prepare
	(ins :condition (Vobject = pending) :subgoal "prepare" :description "If there is nothing to see, start rehearsing!")

	;~ count-circles
	(ins :condition (Vobject = yes) :subgoal "count-circles" :description "If a new screen is up, start a new count")	;; Is this going to blink as new countable items are attended to?
	;~ store-count
	(ins :condition (Vobject = last) :action ((zero WMid) -> newWM (say WMcount) -> AC) :description "No more object, remember count")
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
	(ins :condition (RT1 <> error) :action ((? RTid) -> RT) :description "Rehearse the next item")
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
	(ins :condition (RT1 <> error RTcount <> zero) :action ((? RTid) -> RT (report RTcount) -> AC) :description "Report item and retrieve next")
	;~ finish
	(ins :condition (RTcount = zero) :action (finish -> Gtask) :description "No more items: done")
)


;;; What is the sequence?
;;; Initial: PS empty, V empty
;;; Experimental code puts something in V1: V1 has an object
;;; First instruction puts something in PS1: PS1 has the task
;;; Second instruction gets the property of the object in V1: PS1 task, V1 is empty again, V2 has the property
;;; Retrieval gets the key corresponding to V2: PS1 still has the task, V1 is empty V2 has the property and V3 has the key
;;; After key has been pressed, PS1 has the task, and V1-V3 should be empty (action should clear them)


(add-instr food-task :input (Vobject Vfood Vsize) :variables (WMcur-task WMprev-task WMprev2-task) :declarative ((RTmapping RTcategory RTkey)(RTother RTfirst RTsecond))
:pm-function single-task-AB
:init init-single-task-AB
:reward 10.0
	;~ get-property
	(ins :condition (Vfood = nil) :action ((get-property food-property) -> AC) :description "Task is food so get food property")
	;~ get-key
	(ins :condition (Vfood <> nil RTkey = nil) :action ((mapping Vfood) -> RT) :description "Get food - key mapping")
	;~ press-key
	(ins :condition (RTkey <> nil) :action ((press-key RTkey) -> AC WMprev-task -> WMprev2-task WMcur-task -> WMprev-task blankAB -> WMcur-task) :description "Press appropriate key")
)

(add-instr size-task :input (Vobject Vfood Vsize) :variables (WMcur-task WMprev-task WMprev2-task) :declarative ((RTmapping RTcategory RTkey)(RTother RTfirst RTsecond))
:pm-function single-task-AB
:init init-single-task-AB
:reward 10.0
	;~ get-property
	(ins :condition (Vsize = nil) :action  ((get-property size-property) -> AC) :description "Task is size so get size property")
	;~ get-key
	(ins :condition (Vsize <> nil RTkey = nil) :action ((mapping Vsize) -> RT) :description "Get Size - key mapping")
	;~ press-key
	(ins :condition (RTkey <> nil) :action ((press-key RTkey) -> AC WMprev-task -> WMprev2-task WMcur-task -> WMprev-task blankAB -> WMcur-task) :description "Press appropriate key")
)


(add-instr single-task-A :input (Vobject Vfood Vsize) :variables (WMcur-task WMprev-task WMprev2-task) :declarative ((RTmapping RTcategory RTkey))
:pm-function single-task-AB
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 mapping slot2 vegetable slot3 "s")(isa fact slot1 mapping slot2 fruit slot3 "k"))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)
	;~ init
	(ins :condition (WMcur-task = nil) :action (food-task -> WMcur-task blankAB -> WMprev-task blankAB -> WMprev2-task) :description "Init WM to non-empty nil values so prim copy action can copy it.")
	;~ ready
	(ins :condition (WMcur-task = blankAB) :action (food-task -> WMcur-task) :description "Ready for food-task")
	;~ idle
	(ins :condition (Vobject = pending) :subgoal "idle" :description "Not preparing but waiting")
	;~ food-task
	(ins :condition (Vobject = yes WMcur-task = food-task) :subgoal "food-task" :description "Go to the food task")
	;~ finish
	(ins :condition (Vobject = last) :action (finish -> Gtask) :description "Done with the block")
)


(add-instr single-task-B :input (Vobject Vfood Vsize) :variables (WMcur-task WMprev-task WMprev2-task) :declarative ((RTmapping RTcategory RTkey))
:pm-function single-task-AB
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 mapping slot2 small slot3 "s")(isa fact slot1 mapping slot2 large slot3 "k"))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)
	;~ init
	(ins :condition (WMcur-task = nil) :action (size-task -> WMcur-task blankAB -> WMprev-task blankAB -> WMprev2-task) :description "Init WM to non-empty nil values so prim copy action can copy it.")
	;~ ready
	(ins :condition (WMcur-task = blankAB) :action (size-task -> WMcur-task) :description "Ready for size-task")
	;~ idle
	(ins :condition (Vobject = pending) :subgoal "idle" :description "Not preparing but waiting")
	;~ size-task
	(ins :condition (Vobject = yes WMcur-task = size-task) :subgoal "size-task" :description "Go to the size task")
	;~ finish
	(ins :condition (Vobject = last) :action (finish -> Gtask) :description "Done with the block")
)


(add-instr task-switching-AB :input (Vobject Vfood Vsize) :variables (WMcur-task WMprev-task WMprev2-task) :declarative ((RTmapping RTcategory RTkey)(RTother RTfirst RTsecond))
:pm-function single-task-AB
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 other-task slot2 food-task slot3 size-task)(isa fact slot1 other-task slot2 size-task slot3 food-task))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)
	;~ init
	(ins :condition (WMcur-task = nil) :action (blankAB -> WMcur-task blankAB -> WMprev-task blankAB -> WMprev2-task) :description "Init WM to non-empty nil values so prim copy action can copy it.")

	;~ food-task
	(ins :condition (Vobject = yes WMcur-task = food-task) :subgoal "food-task" :description "Go to the food task")
	;~ size-task
	(ins :condition (Vobject = yes WMcur-task = size-task) :subgoal "size-task" :description "Go to the size task")
	;~ choose-task
	(ins :condition (Vobject = yes WMcur-task = blankAB) :subgoal "ABCD-choose-task" :description "Time to respond, unprepared; find the task to perform")

	;~ prepare
	(ins :condition (Vobject != last WMcur-task = blankAB) :subgoal "prepare" :description "While waiting determine next task")
	;~ idle
	(ins :condition (Vobject != last WMcur-task = blankAB) :subgoal "idle" :description "Not preparing but waiting")

	;~ finish
	(ins :condition (Vobject = last) :action (finish -> Gtask) :description "Done with the block")
)


;;; Now tasks C and D.

(add-instr transport-task :input (Vobject Vtransport Vnumber) :variables (WMcur-task WMprev-task WMprev2-task) :declarative ((RTmapping RTcategory RTkey))
:pm-function single-task-CD
:init init-single-task-AB
:reward 10.0
	;~ get-property
	(ins :condition (Vtransport = nil) :action  ((get-property transport-property) -> AC) :description "Task is transportation so get transport property")
	;~ get-key
	(ins :condition (Vtransport <> nil RTkey = nil) :action ((mapping Vtransport) -> RT) :description "Get Size - key mapping")
	;~ press-key
	(ins :condition (RTkey <> nil) :action ((press-key RTkey) -> AC WMprev-task -> WMprev2-task WMcur-task -> WMprev-task blankCD -> WMcur-task) :description "Press appropriate key")
)

(add-instr number-task :input (Vobject Vtransport Vnumber) :variables (WMcur-task WMprev-task WMprev2-task) :declarative ((RTmapping RTcategory RTkey))
:pm-function single-task-CD
:init init-single-task-AB
:reward 10.0
	;~ get-property
	(ins :condition (Vnumber = nil) :action  ((get-property number-property) -> AC) :description "Task is number so get number property")
	;~ get-key
	(ins :condition (Vnumber <> nil RTkey = nil) :action ((mapping Vnumber) -> RT) :description "Get Size - key mapping")
	;~ press-key
	(ins :condition (RTkey <> nil) :action ((press-key RTkey) -> AC WMprev-task -> WMprev2-task WMcur-task -> WMprev-task blankCD -> WMcur-task) :description "Press appropriate key")
)

(add-instr single-task-C :input (Vobject Vtransport Vnumber) :variables (WMcur-task WMprev-task WMprev2-task) :declarative ((RTmapping RTcategory RTkey))
:pm-function single-task-CD
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 mapping slot2 plane slot3 "s")(isa fact slot1 mapping slot2 car slot3 "k"))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)
	;~ init
	(ins :condition (WMcur-task = nil) :action (transport-task -> WMcur-task blankCD -> WMprev-task blankCD -> WMprev2-task) :description "Init WM to non-empty nil values so prim copy action can copy it.")
	;~ ready
	(ins :condition (WMcur-task = blankCD) :action (transport-task -> WMcur-task) :description "Ready for transport-task")
	;~ idle
	(ins :condition (Vobject = pending) :subgoal "idle" :description "Not preparing but waiting")
	;~ transport-task
	(ins :condition (Vobject = yes WMcur-task = transport-task) :subgoal "transport-task" :description "Go to the transport task")
	;~ finish
	(ins :condition (Vobject = last) :action (finish -> Gtask) :description "Done with the block")
)


(add-instr single-task-D :input (Vobject Vtransport Vnumber) :variables (WMcur-task WMprev-task WMprev2-task) :declarative ((RTmapping RTcategory RTkey))
:pm-function single-task-CD
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 mapping slot2 one slot3 "s")(isa fact slot1 mapping slot2 two slot3 "k"))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)
	;~ init
	(ins :condition (WMcur-task = nil) :action (number-task -> WMcur-task blankCD -> WMprev-task blankCD -> WMprev2-task) :description "Init WM to non-empty nil values so prim copy action can copy it.")
	;~ ready
	(ins :condition (WMcur-task = blankCD) :action (number-task -> WMcur-task) :description "Ready for number-task")
	;~ idle
	(ins :condition (Vobject = pending) :subgoal "idle" :description "Not preparing but waiting")
	;~ number-task
	(ins :condition (Vobject = yes WMcur-task = number-task) :subgoal "number-task" :description "Go to the number task")
	;~ finish
	(ins :condition (Vobject = last) :action (finish -> Gtask) :description "Done with the block")
)

(add-instr task-switching-CD :input (Vobject Vtransport Vnumber) :variables (WMcur-task WMprev-task WMprev2-task) :declarative ((RTmapping RTcategory RTkey)(RTother RTfirst RTsecond))
:pm-function single-task-CD
:init init-single-task-AB
:reward 10.0
:facts ((isa fact slot1 other-task slot2 transport-task slot3 number-task)(isa fact slot1 other-task slot2 number-task slot3 transport-task))
:parameters ((sgp :lf 0.05 :egs 0.04 :rt 0.0 :perception-activation 0.0 :mas nil :alpha 0.03 :imaginal-activation 0.0 )             (spp retrieve-instruction :u 2.0)
(setf *condition-spread* 1.0)             (setf *verbose* nil)
)
	;~ init
	(ins :condition (WMcur-task = nil) :action (blankCD -> WMcur-task blankCD -> WMprev-task blankCD -> WMprev2-task) :description "Init WM to non-empty nil values so prim copy action can copy it.")

	;~ transport-task
	(ins :condition (Vobject = yes WMcur-task = transport-task) :subgoal "transport-task" :description "Go to the transport task")
	;~ number-task
	(ins :condition (Vobject = yes WMcur-task = number-task) :subgoal "number-task" :description "Go to the number task")
	;~ choose-task
	(ins :condition (Vobject = yes WMcur-task = blankCD) :subgoal "ABCD-choose-task" :description "Time to respond, unprepared; find the task to perform")

	;~ prepare
	(ins :condition (Vobject != last WMcur-task = blankCD) :subgoal "prepare" :description "While waiting determine next task")
	;;~ idle
	;(ins :condition (Vobject != last WMcur-task = blankCD) :subgoal "idle" :description "Not preparing but waiting")

	;~ finish
	(ins :condition (Vobject = last) :action (finish -> Gtask) :description "Done with the block")
)

(add-instr ABCD-choose-task :input (Vobject Vfood Vsize) :variables (WMcur-task WMprev-task WMprev2-task) :declarative ((RTmapping RTcategory RTkey)(RTother RTfirst RTsecond))
:pm-function single-task-CD
:init init-single-task-AB
:reward 10.0
	;~ repeat-same-task
	(ins :condition (WMprev-task <> WMprev2-task RTsecond = nil) :action (WMprev-task -> WMcur-task) :description "If the last two trials were the same task, retrieve the other task")
	;~ retrieve-other-task
	(ins :condition (WMprev-task = WMprev2-task RTsecond = nil) :action ((other-task WMprev-task) -> RT) :description "If the last two trials were the same task, retrieve the other task")
	;~ set-task
	(ins :condition (RTsecond <> nil) :action (RTsecond -> WMcur-task) :description "Set the task to the retrieved task")
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
	(ins :condition (Vobject != last RTstimulus != Vcolor) :subgoal "prepare" :description "Prepare while waiting for stimulus")
	;~ idle
	(ins :condition (Vobject != last RTstimulus != Vcolor) :subgoal "idle" :description "Do nothing while waiting for stimulus")

	;~ say-correct
	(ins :condition (RTstimulus = Vcolor) :action ((say RTconcept) -> AC) :description "Say the answer")

	;~ finish
	(ins :condition (Vobject = last) :action (finish -> Gtask) :description "Done with this block")

)

(add-instr stroop-wrong-answer :input (Vobject Vcolor Vword) :declarative ((RTmapping RTstimulus RTconcept RTstim-type))
:pm-function stroop-pm

	;;~ retrieve-word
	;(ins :condition (RTstimulus = nil) :action ((s-mapping Vword) -> RT) :description "Retrieve color concept of the ink color")
	;~ redirect
	(ins :condition (Vtask = stroop) :subgoal "stroop-answer" :description "Notice that is the wrong answer, and try again") ; :condition (RTstimulus = Vword)
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

	;~ count-span
	(ins :condition (Vtask = count-span) :subgoal "cs-rehearse" :description "Prepare for count-span by rehearsing the list")

	;~ AB
	(ins :condition (Vtask = task-switching-AB) :subgoal "ABCD-choose-task" :description "Prepare for AB by selecting next task")
	;~ CD
	(ins :condition (Vtask = task-switching-CD) :subgoal "ABCD-choose-task" :description "Prepare for CD by selecting next task")

	;~ stroop
	(ins :condition (Vtask = stroop) :subgoal "stroop-answer" :description "Prepare for stroop by focusing on color")
)

;;; IDLE : Redirect to another subgoal when the stimulus appears, based on the current task
(add-instr idle :input (Vobject Vcolor Vword) :working-memory (WMconcept WMprev) :declarative ((RTmapping RTstimulus RTconcept RTstim-type))
	;~ wait
	(ins :condition (Vobject = pending) :action ((wait) -> AC) :description "Wait until something happens.")

	;~ AB-focus-task
	(ins :condition (Vtask = task-switching-AB Vobject = yes) :subgoal "ABCD-choose-task" :description "Time to respond, unprepared; find the task to perform")
	;~ CD-focus-task
	(ins :condition (Vtask = task-switching-CD Vobject = yes) :subgoal "ABCD-choose-task" :description "Time to respond, unprepared; find the task to perform")

	;~ focus-stroop-answer
	(ins :condition (Vtask = stroop Vword <> nil) :subgoal "stroop-answer" :description "Go for font color answer")
	;~ wrong-stroop-answer
	(ins :condition (Vtask = stroop Vword = blue-word) :subgoal "stroop-wrong-answer" :description "Try default color word answer")
)
