
(add-instr procedure-a :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;; Using Gparent as a spare slot that won't mess with the linked list of WMids
	;~ init
	(ins :condition (Gtop = nil) :action (WMid -> Gtop) :description "Set up memory for a procedure")
	;~ step-1
	(ins :condition (Gtop <> nil Gparent = nil) :subgoal "solid-lime-diff") :description "Starting step 1 in procedure A")
	;~ remember-particulate
	(ins :condition (Gparent = solid-lime-diff) :action (particulate -> WMatt (? ? WMid) -> newWM particulate -> Gparent) :description "Remember particulate")
	;~ step-2
	(ins :condition (Gparent = particulate) :subgoal "greater-algae" :description "Starting step 2 in procedure A")
	;~ step-3
	(ins :condition (Gparent = greater-algae) :subgoal "part-plus-mineral" :description "Starting step 3 in procedure A")
	;~ remember-index1
	(ins :condition (Gparent = part-plus-mineral) :action (index1 -> WMatt (? ? WMid) -> newWM index1 -> Gparent) :description "Remember index1")
	;~ step-4
	(ins :condition (Gparent = index1) :subgoal "mean-toxin" :description "Starting step 4 in procedure A")
	;~ step-5
	(ins :condition (Gparent = mean-toxin) :subgoal "index1-div-marine" :description "Starting step 5 in procedure A")
	;~ step-6
	(ins :condition (Gparent = index1-div-marine) :subgoal "index2-min-mineral" :description "Starting step 6 in procedure A")
	;~ step-7
	(ins :condition (Gparent = index2-min-mineral) :action (finish -> Gtask) :description "Entering last result and finishing procedure A")
)

(add-instr solid-lime-diff :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))

	;~ step-1
	(ins :condition (Vlabel = nil) :action ( (read lime4) -> AC ) :description "Entering solid-lime-diff, reading lime4")
	;~ step-2
	(ins :condition (Vlabel = lime4) :action (Vvalue -> WMvalue (read lime2) -> AC) :description "Reading lime2")
	;~ diff
	(ins :condition (RT1 = nil   Vlabel = lime2) :action ((subtract WMvalue Vvalue) -> RT (read solid) -> AC) :description "Calculating the difference and reading Solid")
	;~ mult
	(ins :condition (RTtype = subtract  Vlabel = solid ) :action ((mult Vvalue RTans) -> RT) :description "Multiplying the result by Solid")
	;~ finish
	(ins :condition (RTtype = mult) :action ((enter RTans) -> AC solid-lime-diff -> WMatt RTans -> WMvalue solid-lime-diff -> Gparent) :description "Finishing solid-lime-diff")
)

(add-instr greater-algae :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ start
	(ins :condition (Vlabel = nil) :action ( (read algae) -> AC) :description "Entering greater-algae, reading Algae")
	;~ div-2
	(ins :condition (RT1 = nil Vlabel = algae) :action ((div Vvalue 2) -> RT) :description "Dividing algae by 2")
	;~ read-solid
	(ins :condition (RTans <> nil Vlabel = algae) :action (RTans -> WMvalue (read solid) -> AC) :description "Reading solid")
	;~ div-3
	(ins :condition (RT1 = nil Vlabel = solid) :action ((div Vvalue 3) -> RT) :description "Dividing solid by 3")
	;~ greater
	(ins :condition (RTtype = div Vlabel = solid) :action ((greater-of WMvalue RTans) -> RT) :description "Determining the greater of the two")
	;~ finish
	(ins :condition (RTtype = greater-of) :action (greater-algae -> WMatt RTans -> WMvalue greater-algae -> Gparent) :description "Finishing up greater-algae")
)

(add-instr part-plus-mineral :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
;~ start
	(ins :condition (RT1 = nil) :action (mineral -> WMatt Gtop -> RTid) :description "Enter part-plus-mineral, retrieving top WM item")
	;~ add
	(ins :condition (RTatt = particulate) :action ((add RTvalue WMvalue) -> RT WMid -> Gtop) :description "Add the retrieved particulate to stored Mineral & Store mineral as new top")
	;~ finish
	(ins :condition (RTtype = add) :action ((enter RTans) -> AC (part-plus-mineral RTans WMid) -> newWM part-plus-mineral -> Gparent) :description "Finishing up part-plus-mineral")
)

(add-instr mean-toxin :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ start
	(ins :condition (Vlabel = nil) :action ((read toxinmax) -> AC) :description "Entering mean-toxin, determining toxinmax")
	;~ toxinmin
	(ins :condition (Vlabel = toxinmax) :action (Vvalue -> WMvalue (read toxinmin) -> AC) :description "Determining toxinmin")
	;~ add
	(ins :condition (RT1 = nil Vlabel = toxinmin) :action ((add WMvalue Vvalue) -> RT) :description "Add them to each other")
	;~ divide
	(ins :condition (RTtype = add) :action ((div RTans 2) -> RT) :description "Divide the result by 2")
	;~ finish
	(ins :condition (RTtype = div) :action ((enter RTans) -> AC mean-toxin -> WMatt RTans -> WMvalue mean-toxin -> Gparent) :description "Finishing up Mean Toxin")
)

(add-instr index1-div-marine input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ start
	(ins :condition (RT1 = nil) :action (marine -> WMatt Gtop -> RTid) :description "Entering Index1-div-marine, retrieving top DM item")
	;~ next
	(ins :condition (RTatt = mineral) :action (RTnext -> RTid)  :description "If retrieved item is not index1 then retrieve next")
	;~ divide
	(ins :condition (RTatt = index1) :action ((div RTvalue WMvalue) -> RT) :description "If it is index1 then divide it by marine which is still in WM")
	;~ finish
	(ins :condition (RTtype = div) :action ((enter RTans) -> AC index1-div-marine -> WMatt RTans -> WMvalue index1-div-marine -> Gparent) :description "Finishing up index1-div-marine")
)

(add-instr index2-min-mineral input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ start
	(ins :condition (RT1 = nil) :action (index2 -> WMatt Gtop -> RTid) :description "Entering Index2-min-mineral, retrieving top DM item")
	;~ subtract
	(ins :condition (RTatt = mineral) :action ((subtract WMvalue RTvalue) -> RT) :description "Subtract stored Index2 from the retrieved mineral")
	;~ finish
	(ins :condition (RTtype = subtract) :action ((enter RTans) -> AC index2-min-mineral -> WMatt RTans -> WMvalue index2-min-mineral -> Gparent) :description "Finishing up index2-min-mineral")
)

(add-instr mineral-div-marine :input (Vlabel Vvalue) :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ start
	(ins :condition (RT1 = nil) :action (marine -> WMatt Gtop -> RTid) :description "Entering mineral-div-marine, retrieving top DM item")
	;~ skip
	(ins :condition (RTatt = particulate) :action ( (mineral ? RTid) -> RT) :description "Found particulate,  move on")
	;~ divide
	(ins :condition (RTatt = mineral) :action ((div RTvalue WMvalue) -> RT) :description "Found mineral, enter it in the division")
	;~ finish
	(ins :condition (RTtype = div) :action ((enter RTans) -> AC mineral-div-marine -> WMatt RTans -> WMvalue mineral-div-marine -> Gparent) :description "Finishing up mineral-div-marine")
)


(add-instr procedure-b :input (Vlabel Vvalue) :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ init
	(ins :condition (Gtop = nil) :action (WMid -> Gtop) :description "Set up memory for a procedure")
	;~ start
	(ins :condition (Gtop <> nil Gparent = nil) :subgoal "mean-toxin" :description "Starting step 1 in procedure B")
	;~ remember-particulate
	(ins :condition (Gparent = mean-toxin) :action (particulate -> WMatt (? ? WMid) -> newWM particulate -> Gparent) :description "Remember particulate")
	;~ step-2
	(ins :condition (Gparent = particulate) :subgoal "solid-lime-diff" :description "Starting step 2 in procedure B")
	;~ remember-mineral
	(ins :condition (Gparent = solid-lime-diff) :action (mineral -> WMatt (? ? WMid) -> newWM mineral -> Gparent) :description "Remember mineral")
	;~ step-3
	(ins :condition (Gparent = mineral) :subgoal "greater-algae" :description "Starting step 3 in procedure B")
	;~ step-4
	(ins :condition (Gparent = greater-algae) :subgoal "mineral-div-marine" :description "Starting step 4 in procedure B")
	;~ step-5
	(ins :condition (Gparent = mineral-div-marine) :subgoal "part-mult-index1" :description "Starting step 5 in procedure B")
	;~ step-6
	(ins :condition (Gparent = part-mult-index1) :subgoal "index1-plus-index2" :description "Starting step 6 in procedure B")
	;~ finish
	(ins :condition (Gparent = index1-plus-index2) :action (finish -> Gtask) :description "Finishing procedure B")
)


(add-instr triple-lime :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ start
	(ins :condition (Vlabel = nil) :action ( (read limemin) -> AC ))
	;~ mult
	(ins :condition (RT1 = nil Vlabel = limemin ) :action ((mult Vvalue 3) -> RT))
	;~ read
	(ins :condition (RTans <> nil Vlabel = limemin) :action (RTans -> WMvalue (read algae) -> AC))
	;~ add
	(ins :condition (RT1 = nil Vlabel = algae ) :action ((add WMvalue Vvalue) -> RT))
	;~ finish
	(ins :condition (RTtype = add ) :action ((enter RTans) -> AC triple-lime -> WMatt RTans -> WMvalue triple-lime -> Gparent))
)

(add-instr lesser-evil :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ read-solid
	(ins :condition (Vlabel = nil) :action ((read solid) -> AC))
	;~ read-lime1
	(ins :condition (Vlabel = solid) :action (Vvalue -> WMvalue (read lime1) -> AC))
	;~ add-lime1
	(ins :condition (RT1 = nil Vlabel = lime1) :action ((add WMvalue Vvalue) -> RT))
	;~ read-algae
	(ins :condition (RTtype = add Vlabel = lime1) :action (RTans -> WMvalue intermediate -> WMatt (? ? WMid) -> newWM  (read algae) -> AC)) ;; We can only remember one intermediate result, so the second needs to go to DM
	;~ read-toxin3
	(ins :condition (Vlabel = algae) :action (Vvalue -> WMvalue (read toxin3) -> AC))
	;~ add-toxin3
	(ins :condition (RT1 = nil Vlabel = toxin3) :action ((add WMvalue Vvalue) -> RT))
	;~ toxin3
	(ins :condition (RTtype = add Vlabel = toxin3) :action  (RTans -> WMvalue WMprev -> RTid))
	;~ read-intermediate
	(ins :condition (RTatt = intermediate) :action (RTprev -> WMprev (greater-than WMvalue RTvalue) -> RT))
	;~ greater-than-true
	(ins :condition (RTtype = greater-than RTans = true) :action ((enter RTarg1) -> AC RTarg1 -> WMvalue lesser-evil -> WMatt lesser-evil -> Gparent))
	;~ greater-than-false
	(ins :condition (RTtype = greater-than RTans = false) :action ((enter RTarg2) -> AC RTarg2 -> WMvalue lesser-evil -> WMatt lesser-evil -> Gparent))
)

(add-instr solid-div-lime  :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ read-solid
	(ins :condition (Vlabel = nil)  :action ((read solid) -> AC))
	;~ read-lime1
	(ins :condition (Vlabel = solid) :action (Vvalue -> WMvalue (read lime1) -> AC))
	;~ div-lime1
	(ins :condition (RT1 = nil Vlabel = lime1) :action ((div WMvalue Vvalue) -> RT))
	;~ finish
	(ins :condition (RTtype = div) :action ((enter RTans) -> AC solid-div-lime -> WMatt RTans -> WMvalue solid-div-lime -> Gparent))
)

(add-instr part-mult-index1 :input (Vlabel Vvalue) :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ start
	(ins :condition (RT1 = nil) :action (index1 -> WMatt Gtop -> RTid) :description "Entering part-mult-index1, retrieving top DM item")
	;~ mult
	(ins :condition (RTatt = particulate) :action ((mult RTvalue WMvalue) -> RT WMid -> Gtop (part-mult-index1) -> newWM) :description "Multiplying, and index1 is new top")
	;~ finish
	(ins :condition (RTtype = mult) :action ((enter RTans) -> AC RTans -> WMvalue part-mult-index1 -> Gparent))
)

(add-instr index1-plus-index2 :input (Vlabel Vvalue) :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ start
	(ins :condition (RT1 = nil) :action (index2 -> WMatt Gtop -> RTid) :description "Entering part-mult-index1, retrieving top DM item")
	;~ add
	(ins :condition (RTatt = index1) :action ((add WMvalue RTvalue) -> RT))
	;~ finish
	(ins :condition (RTtype = add) :action ((enter RTans) -> AC index1-plus-index2 -> WMatt RTans -> WMvalue index1-plus-index2 -> Gparent))
)


(add-instr procedure-c :input (Vlabel Vvalue) :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev)) 
	;~ init
	(ins :condition (Gtop = nil) :action (WMid -> Gtop) :description "Set up memory for a procedure")
	;~ start
	(ins :condition (Gtop <> nil Gparent = nil) :subgoal "triple-lime" :description "Starting step 1 in procedure C")
	;~ remember-particulate
	(ins :condition (Gparent = triple-lime) :action (particulate -> WMatt (? ? WMid) -> newWM particulate -> Gparent) :description "Remember particulate")
	;~ step-2
	(ins :condition (Gparent = particulate) :subgoal "lesser-evil" :description "Starting step 2 in procedure C")
	;~ step-3
	(ins :condition (Gparent = lesser-evil) :subgoal "part-plus-mineral" :description "Starting step 3 in procedure C")
	;~ remember-index1
	(ins :condition (Gparent = part-plus-mineral) :action (index1 -> WMatt (? ? WMid) -> newWM index1 -> Gparent) :description "Remember index1")
	;~ step-4
	(ins :condition (Gparent = index1) :subgoal "solid-div-lime" :description "Starting step 4 in procedure C")
	;~ step-5
	(ins :condition (Gparent = solid-div-lime) :subgoal "index1-div-marine" :description "Starting step 5 in procedure C")
	;~ step-6
	(ins :condition (Gparent = index1-div-marine) :subgoal "index2-min-mineral" :description "Starting step 6 in procedure C")
	;~ step-7
	(ins :condition (Gparent = index2-min-mineral) :action (finish -> Gtask) :description "finishing procedure C")
)


(add-instr procedure-d :input (Vlabel Vvalue) :input (Vlabel Vvalue) :variables (WMatt WMvalue WMprev) :declarative ((RTtype RTarg1 RTarg2 RTans)(RTatt RTvalue RTprev))
	;~ init
	(ins :condition (Gtop = nil) :action (WMid -> Gtop) :description "Set up memory for a procedure")
	;~ start
	(ins :condition (Gtop <> nil Gparent = nil) :subgoal "triple-lime" :description "Starting step 1 in procedure D")
	;~ remember-particulate
	(ins :condition (Gparent = triple-lime) :action (particulate -> WMatt (? ? WMid) -> newWM particulate -> Gparent) :description "Remember particulate")
	;~ step-2
	(ins :condition (Gparent = particulate) :subgoal "lesser-evil" :description "Starting step 2 in procedure D")
	;~ remember-mineral
	(ins :condition (Gparent = lesser-evil) :action (mineral -> WMatt (? ? WMid) -> newWM mineral -> Gparent) :description "Remember mineral")
	;~ step-3
	(ins :condition (Gparent = mineral) :subgoal "solid-div-lime" :description "Starting step 3 in procedure D")
	;~ step-4
	(ins :condition (Gparent = solid-div-lime) :subgoal "mineral-div-marine" :description "Starting step 4 in procedure D")
	;~ step-5
	(ins :condition (Gparent = mineral-div-marine) :subgoal "part-mult-index1" :description "Starting step 5 in procedure D")
	;~ step-6
	(ins :condition (Gparent = part-mult-index1) :subgoal "index1-plus-index2" :description "Starting step 6 in procedure D")
	;~ step-7
	(ins :condition (Gparent = index1-plus-index2) :action (finish -> Gtask) :description "finishing procedure D")
)

