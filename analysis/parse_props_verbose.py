# -*- coding: utf-8 -*-
"""
Created on Wed Mar  8 09:38:37 2017

@author: bryan
"""

import re
import os  # For deleting old file in splitFile()

"""
A utility to split a file into two according to the given regex patterns.
If a line in the source file has reg1, put it in dst_path1.
Else if a line in the source file has reg2, put it in dst_path2.
If all lines match either reg1 or reg2, the source file is then deleted from disk.
"""
def splitFile(src_path, reg1, reg2, dst_path1, dst_path2):
    allMatch = True
    dstFile1 = None
    dstFile2 = None
    
    # Open source file
    srcFile = open(src_path)
    
    for line in srcFile:
        if re.match(r''+reg1, line):
            if not dstFile1:
                dstFile1 = open(dst_path1, "w+")
            dstFile1.write(line)
        elif not reg2 or re.match(r''+reg2, line):
            if not dstFile2:
                dstFile2 = open(dst_path2, "w+")
            dstFile2.write(line)
        else:
            allMatch = False
    
    srcFile.close()
    if dstFile1:
        dstFile1.close()
    if dstFile2:
        dstFile2.close()
    
    if not allMatch:
        try:
            os.remove(src_path)
        except OSError, e:
            print ("Error deleting %s : %s - %s." % (src_path,e.filename,e.strerror))


# Go through the given agent trace file and count various behavioral stats.
# Output is in a delimiter-separated file, each row being a trial within the task, each column being the stat during that trial.
#   inpath - the filepath of the input file
#   outpath - the filepath to print to (creates if not exists, appends if does)
#   timepath - (optional) the filepath of act-r agent timings to copy for ltm retrievals
#   actions - any string keywords that indicate an agent action that concludes a trial within the task (mark to stop collecting data for a row of output)
#   countOp - (optional) the name of an operator to keep special track of how many times it is selected within a trial
def convertData(inpath, outpath, timepath, actions, countOp):
    
    #DC_TIME = 0.05
    
    DELIM = "\t"
    COUNT_IMPASSES = True
    
    #PRIM_TIME = 0.4     # Add DECLARATIVE retrieval time for each primitive
    LTM_TIME = 0.0      # How much time to allot for long-term memory retrievals
    
    printing = False    # Marks whether the program is printing to file right now
    rowentry = 0        # When printing, the index of the results being printed
    
    proc_dc = 0         # The count of decision cycles this trial
    ltmTimeBuff = 0.0   # The sum of LTM retrieval times this trial
    ltm_count = 0       # The count of LTM retrievals this trial
    p_count = 0         # The count of chunks learned
    fail_count = 0      # The count of how many times the agent retrieved the wrong thing
    
    countedOpCount = 0
    
    rows = []

    actrfile = None

    infile = open(inpath)
    if timepath:
        actrfile = open(timepath)
        #PRIM_TIME = float(actrfile.readline().split()[-1])
        #LTM_TIME = float(actrfile.readline().split()[-1]) #PRIM_TIME - (DC_TIME * 2.0)
    
    for line in infile:
        opLabInd = line.find(' O: ')        # If found, this is an operator
        impLabInd = line.find('==>S: ')     # If found, this is an impasse cycle
        echoInd = line.find('echo')         # The SML environment prints to file with the "echo" cmd command. Append to these lines for file output.
        
        # Count normal decisions
        if opLabInd >= 0 or impLabInd >= 0:
            opNameInd = line.find('(')  # Find the index in the line where the operator name starts
            if COUNT_IMPASSES or opLabInd != -1:
                proc_dc += 1
            # Count number of LTM retrievals, separate from decision cycles
            if re.match(r'props-load', line[opNameInd+1:]):
                ltm_count += 1
                #proc_dc -= 2    # Undo the DC count for the query and for the corresponding 'retrieve' operator. (if not also including the 50ms for retrieval DCs)
            if countOp and re.match(r''+countOp, line[opNameInd+1:]):   # Count special operator appearances, if any
                countedOpCount += 1
        # Count new productions
        elif line.startswith("Learning new rule chunk"):
            p_count += 1
        # Don't count chunks that were reverted to justifications due to lack of confidence
        elif line.startswith("Chunk confidence at"):
            p_count -= 1
        # Count invalid instruction retrievals
        elif line.startswith(" *** RETRACTING"):    # This is a PROPs agent output keyword
            fail_count += 1
        # Notice end of line instruction
        elif line.startswith("Say: "):  # The PROPs agent always precedes output with "Say: "
            if any(a in line[5:] for a in actions):
                # Calculate the appropriate fetching latencies
                if (actrfile):
                    LTM_TIME = float(actrfile.readline().split()[-1])
                #LTM_TIME = PRIM_TIME - (DC_TIME * 2.0)     # This could give a negative number, but that still offsets the 2 DCs for each query/collect
                ltmTimeBuff = LTM_TIME*ltm_count # + PRIM_TIME*prim_count
                
                # Add data point - each corresponds to a completed task procedure
                rows += [(proc_dc, p_count, ltmTimeBuff, ltm_count, fail_count, countedOpCount)]
                
                # Reset for the next entry
                ltmTimeBuff = 0.0;
                ltm_count = 0
                #prim_count = 0
                proc_dc = 0
                fail_count = 0
                countedOpCount = 0
        # If reaching an "echo" output, we're at the end of a task procedure, and can flush 'rows' to file
        elif echoInd >= 0 and len(rows) > 0:
            if not printing:
                printing = True
                f = open(outpath, "a")
            
            entry = rows[rowentry]
            ln = line[echoInd+5:].split()+[None, None, None, None]
            origlen = len(ln)
            
            # These are the actual column entries printed:
            ln[origlen-4] = str(entry[0])                                               # Counted DCs
            ln[origlen-3] = '{0:.4f}'.format(entry[2])                                  # LTM Time
            ln[origlen-2] = str(entry[3])                                               # LTM Count
            #ln[origlen-1]= str(entry[4])                                                # Fails
            ln[origlen-1]= str(entry[5])                                                # Counted ops
            
            ln = DELIM.join(ln)
            newline = ln + "\n"
            f.write(newline)
            
            rowentry += 1
        
        # Reset the counts after all is flushed, and start counting the next set
        elif printing and line.startswith('***'):   # '*** TASK SET TO ...' at the start of the next task
            # Just finished procedure
            printing = False
            f.close()
            #print "Line: rows =", len(rows), ", rowentry =", rowentry  # Verify that record division are correct: these should be equal
            rows = []
            rowentry = 0
            
            # Reset for the next entry
            ltmTimeBuff = 0.0;
            ltm_count = 0
            proc_dc = 0
            fail_count = 0
            countedOpCount = 0
            
    # All done, close up
    if printing:
        printing = False
        f.close()
    infile.close()
    if timepath:
        actrfile.close()



def main():
    
    '''
    pathdir = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/elio/results/'
    fetchTimeLatenciesFile = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/elio/elio-out8_X.dat"
    domain = 'elio_props'
    actions = ['enter']
    '''
    pathdir = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/editors/results/'
    fetchTimeLatenciesFile = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/editors/editor_out2_X.dat"
    domain = 'editors_props'
    actions = ['next-instruction']
    countOp = None
    '''
    pathdir = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/cheinmorrison/results/sweep_20190408_states/'
    fetchTimeLatenciesFile = None
    domain = 'stroopChein'
    actions = ['type', 'say']
    countOp = 'prepare'
    '''
    
    # File name generation (can loop over different files)
    conv_types = ['l1']
    T = ['96']
    samples = ['2']
    param3 = [''] #['_lr0025','_lr0050','_lr0075','_lr0100','_lr0125','_lr0150','_lr0175','_lr0200']
    param4 = [''] #['_dr700','_dr725','_dr750','_dr775','_dr800']
    
    for ctype in conv_types:
        for t in T:
            for p3 in param3:
                for p4 in param4:
                    for sample in samples:
                        inpath = pathdir + 'verbose_' + domain + '_' + ctype + '_t' + t + p3 + p4 + '_s' + sample + '.dat'
                        outpath = inpath[:-4] + "_X.dat"
                        
                        # Take the agent trace output file, parse it, and create a new csv with the measured data
                        convertData(inpath, outpath, fetchTimeLatenciesFile, actions, countOp)
                        
                        # If original agent trace output includes multiple kinds of task output, split them into separate files
                        #splitFile(outpath, r'\ASTROOP', None,
                        #          pathdir+ 'stroopChein' +'_'+ctype+'_t'+t+p3+p4+'_s'+sample+'_X.dat',
                        #          pathdir+ 'WMChein' +'_'+ctype+'_t'+t+p3+p4+'_s'+sample+'_X.dat')
    

    
if __name__ == "__main__": main()
