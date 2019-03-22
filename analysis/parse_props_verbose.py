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

def convertData(inpath, outpath, timepath, domain, actions, countOp):
    
    #DC_TIME = 0.05
    
    DELIM = "\t"
    COUNT_IMPASSES = True
    
    #PRIM_TIME = 0.4     # Add DECLARATIVE retrieval time for each primitive
    LTM_TIME = 0.0
    
    printing = False
    lastProc = ""
    sample = 0
    rowentry = 0
    proc_dc = 0
    ltmTimeBuff = 0.0
    ltm_count = 0
    #prim_count = 0
    p_count = 0
    fail_count = 0
    
    countedOpCount = 0
    
    rows = []

    actrfile = None

    infile = open(inpath)
    if timepath:
        actrfile = open(timepath)
        #PRIM_TIME = float(actrfile.readline().split()[-1])
        #LTM_TIME = float(actrfile.readline().split()[-1]) #PRIM_TIME - (DC_TIME * 2.0)
    
    for line in infile:
        opLabInd = line.find(' O: ')
        impLabInd = line.find('==>S: ')
        echoInd = line.find('echo')
        
        # Count normal decisions
        if opLabInd >= 0 or impLabInd >= 0:
            opNameInd = line.find('(')
            if COUNT_IMPASSES or opLabInd != -1:
                proc_dc += 1
            # Count simulated LTM retrieval time, separate from decision cycle time
            if re.match(r'props-load', line[opNameInd+1:]):
                ltm_count += 1
                #proc_dc -= 2    # Undo the count for the query and for the corresponding 'retrieve' operator            
            # Count simulated instruction element retrieval time, separate from decision cycle time
            #if re.match(r'props-evaluate', line[opNameInd+1:]):
            #    prim_count += 1
            if countOp and re.match(r''+countOp, line[opNameInd+1:]):
                countedOpCount += 1
        # Count new productions
        elif line.startswith("Learning new rule chunk"):
            p_count += 1
        # Don't count chunks that were reverted to justifications due to lack of confidence
        elif line.startswith("Chunk confidence at"):
            p_count -= 1
        # Count invalid instruction retrievals
        elif line.startswith(" *** RETRACTING"):
            fail_count += 1
        # Notice end of line instruction
        elif line.startswith("Say: "):
            if any(a in line[5:] for a in actions):
                # Calculate the appropriate fetching latencies
                if (actrfile):
                    LTM_TIME = float(actrfile.readline().split()[-1])
                #LTM_TIME = PRIM_TIME - (DC_TIME * 2.0)     # This could give a negative number, but that still offsets the 2 DCs for each query/collect
                ltmTimeBuff = LTM_TIME*ltm_count # + PRIM_TIME*prim_count
                # Add data point - each corresponds to a completed procedure
                rows += [(proc_dc, p_count, sample, ltmTimeBuff, ltm_count, fail_count, countedOpCount)]
                # Reset for the next entry
                ltmTimeBuff = 0.0;
                ltm_count = 0
                #prim_count = 0
                proc_dc = 0
                fail_count = 0
                countedOpCount = 0
        # Notice change in procedure
        elif echoInd >= 0 and len(rows) > 0:
            if not printing:
                printing = True
                f = open(outpath, "a")
            
            entry = rows[rowentry]
            ln = line[echoInd+5:].split()+[None, None, None, None]
            origlen = len(ln)
            ln[origlen-4] = str(entry[0])                                               # Counted DCs
            ln[origlen-3] = '{0:.4f}'.format(entry[3])                                  # LTM Time
            ln[origlen-2] = str(entry[4])                                               # LTM Count
            #ln[origlen-1]= str(entry[5])                                                # Fails
            ln[origlen-1]= str(entry[6])                                                # Counted ops
            
            ln = DELIM.join(ln)
            newline = ln + "\n"
            f.write(newline)
            
            rowentry += 1
            
        elif printing and line.startswith('***'):   # '*** TASK SET TO ...' at the start of the next task
            # Just finished procedure
            printing = False
            f.close()
            #print "Line: rows =", len(rows), ", rowentry =", rowentry  # Verify that record division are correct: these should be equal
            rows = []
            rowentry = 0
            if lastProc != "a":
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
    
    pathdir = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/editors/results/'
    fetchTimeLatenciesFile = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/editors/editor_out2_X.dat"
    domain = 'editors_props'
    actions = ['next-instruction']
    '''
    
    pathdir = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/cheinmorrison/results/'
    fetchTimeLatenciesFile = None
    domain = 'stroopChein'
    actions = ['type', 'say']
    countOp = 'prepare'
    
    conv_types = ['l12']
    samples = ['4']
    
    T = ['1']
    
    for ctype in conv_types:
        for t in T:
            for sample in samples:
                inpath = pathdir + 'verbose_' + domain + '_' + ctype + '_t' + t + '_s' + sample + '.dat'
                #inpath = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/cheinmorrison/workspace/CheinMorrisonEnvironment/verbose_AgentOutput.txt'
                outpath = inpath[:-4] + "_X.dat"
                convertData(inpath, outpath, fetchTimeLatenciesFile, domain, actions, countOp)
                
                splitFile(outpath, r'\ASTROOP', None,
                          pathdir+ 'stroopChein' +'_'+ctype+'_t'+t+'_s'+sample+'_X.dat',
                          pathdir+ 'WMChein' +'_'+ctype+'_t'+t+'_s'+sample+'_X.dat')
    

    
if __name__ == "__main__": main()
