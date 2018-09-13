# -*- coding: utf-8 -*-
"""
Created on Wed Apr 26 22:37:38 2017

@author: bryan

Takes a text output file from Soar performing a task, 
and creates a declarative SMEM sequence for the observed sequence of operators.
"""

def getOperatorNames(lispPath, taskName):
    names = {}
    header = taskName
    name1 = ""
    name2 = ""
    
    infile = open(lispPath)
    for line in infile:
        currline = line.strip()
        if currline.startswith('(add-instr '):
            header = taskName + '*' + currline.split()[1] + '*'
        elif currline.startswith(';~'):
            if currline.startswith(';~~ '):
                if (name1 == ""):
                    name2 = currline[4:]
                else:
                    name2 = "*" + currline[4:]
            else:
                name1 = currline[3:]
                name2 = ""
        elif currline.startswith('(ins :'):
            names[(header+name1+name2).replace('-','*')] = header+name1+name2      # Make a map of the all '*' version to the proper version
    return names

def generateManualSequence(fileRoot, taskNames, opNames):
    outlines = []
    outlines += ['\n\n### MANUAL PROPS RULE SEQUENCING ###\n\n\n']
    outlines += ['smem --add { (<1> ^props-static props-static\n']
    for i, task in enumerate(taskNames):
        outlines += ['\t^' + task + ' <R' + str(i) + '>\n']
    outlines += [')']
    
    for i, task in enumerate(taskNames):
        idNum = 1
        infile = open(fileRoot + task + '.txt')
        operators = [opNames.get(x) for x in [line[15:-2].replace('-','*').replace('(','').replace(' ','') for line in infile if line.startswith('O: N',8)]]
        infile.close()
        
        outlines += ['\n\n#\t' + task.upper() + '\n']
        # First op becomes the head
        outlines += ['(<R' + str(i) + '> ^name propose*' + operators[0] + '\n\t^next <' + task + '-' + str(idNum) + '>)\n\n']
        outlines += ['(<' + task + '-' + str(idNum) + '> ^name apply*' + operators[0]]
        outlines += ['\n\t^next <' + task + '-' + str(idNum+1) + '>)\n']
        idNum += 1
        # Do the rest - don't include the last finish operator
        for op in operators[1:]:
            outlines += ['(<' + task + '-' + str(idNum) + '> ^name propose*' + op]
            outlines += ['\n\t^next <' + task + '-' + str(idNum+1) + '>)\n']
            idNum += 1
            outlines += ['(<' + task + '-' + str(idNum) + '> ^name apply*' + op]
            outlines += ['\n\t^next <' + task + '-' + str(idNum+1) + '>)\n']
            idNum += 1
        # Close off the sequence
        outlines[-1] = '\n\t^next nil)\n\n'
    outlines += ['}']
    
    # Save the collective output results
    outpath = fileRoot + "agent_fetch_procedures.soar"
    outfile = open(outpath, 'w')
    outfile.writelines(outlines)
    outfile.close()


def main():
    path = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/editors/'
    taskNames = ['ed_1', 'ed_2', 'ed_3', 
                 'edt_1', 'edt_2', 'edt_3', 
                 'emacs_1', 'emacs_2', 'emacs_3']
    opNames = getOperatorNames(path+'editors.lisp', 'editors')
    for fn in taskNames:
        generateManualSequence(path+'editors_', taskNames, opNames)
    

    
if __name__ == "__main__": 
    main()
