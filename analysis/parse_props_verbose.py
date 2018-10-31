# -*- coding: utf-8 -*-
"""
Created on Wed Mar  8 09:38:37 2017

@author: bryan
"""

import re

def convertData(path, timepath, domain, actions):
    
    DC_TIME = 0.05
    
    DELIM = "\t"
    COUNT_IMPASSES = True
    
    PRIM_TIME = 0.4     # Add DECLARATIVE retrieval time for each primitive
    
    printing = False
    lastProc = ""
    sample = 0
    rowentry = 0
    proc_dc = 0
    ltmTimeBuff = 0.0
    ltm_count = 0
    prim_count = 0
    p_count = 0
    fail_count = 0
    
    rows = []

    outpath = path[:-4] + "_X.dat"

    infile = open(path)
    if timepath:
        actrfile = open(timepath)
        #PRIM_TIME = float(actrfile.readline().split()[-1])
    LTM_TIME = PRIM_TIME - (DC_TIME * 2.0)
    
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
            if re.match(r'props-query', line[opNameInd+1:]):
                ltm_count += 1
            # Count simulated instruction element retrieval time, separate from decision cycle time
            if re.match(r'props-evaluate', line[opNameInd+1:]):
                prim_count += 1
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
                PRIM_TIME = float(actrfile.readline().split()[-1])
                LTM_TIME = PRIM_TIME - (DC_TIME * 2.0)     # This could give a negative number, but that still offsets the 2 DCs for each query/collect
                ltmTimeBuff = PRIM_TIME*prim_count + LTM_TIME*ltm_count
                # Add data point - each corresponds to a completed procedure
                rows += [(proc_dc, p_count, sample, ltmTimeBuff, ltm_count, fail_count)]
                # Reset for the next entry
                ltmTimeBuff = 0.0;
                ltm_count = 0
                prim_count = 0
                proc_dc = 0
                fail_count = 0
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
            ln[origlen-1]= str(entry[5])                                                # Fails
            
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
                p_count = 0
    # All done, close up
    if printing:
        printing = False
        f.close()
    infile.close()
    if timepath:
        actrfile.close()



def main():
    pathdir = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/elio/results/'
    fetchTimeLatenciesFile = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/elio/elio-out8_X.dat"
    domain = 'elio'
    actions = ['enter']
    
    '''
    pathdir = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/editors/results/'
    fetchTimeLatenciesFile = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/editors/editor_out2_X.dat"
    domain = 'editors'
    actions = ['next-instruction']
    '''
    
    conv_types = ['l12se']
    samples = ['8']
    
    T = ['10']
    
    for ctype in conv_types:
        for t in T:
            for sample in samples:
                convertData(pathdir + 'verbose_' + domain + '_props_' + ctype + '_t' + t + '_s' + sample + '.dat', 
                            fetchTimeLatenciesFile, domain, actions)
    

    
if __name__ == "__main__": main()
