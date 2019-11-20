# -*- coding: utf-8 -*-
"""
Created on Wed Mar  8 09:38:37 2017

@author: bryan
"""

import re

def convertData(path, timepath, domain, actions):
    
    DC_TIME = 0.05
    
    DELIM = "\t"
    SHIFT= 0.0
    NO_FAILS = False
    
    PRIM_TIME = 0.4     # Add DECLARATIVE retrieval time for each primitive
    
    printing = False
    lastProc = ""
    sample = 0
    rowentry = 0
    proc_dc = 0
    ltmTimeBuff = 0.0
    ltm_count = 0
    prim_count = 0
    fail_count = 0
    latencies = 0.0
    lastSuccessDC = 0
    lastSuccessTime = 0.0
    p_count = 0
    p_total = 0
    p_type = 'nil'
    p_prp_count = 0
    p_prp_total = 0
    p_apl_count = 0
    p_apl_total = 0
    p_cnd_count = 0
    p_cnd_total = 0
    p_aut_count = 0
    p_aut_total = 0
    
    rows = []

    if NO_FAILS:
        outpath = path[:-4] + "_Xnf.dat"
    else:
        outpath = path[:-4] + "_C.dat"

    infile = open(path)
    if timepath:
        actrfile = open(timepath)
        #PRIM_TIME = float(actrfile.readline().split()[-1])
    LTM_TIME = PRIM_TIME - (DC_TIME * 4.0)
    
    for line in infile:
        opNameInd = line.find('(')
        echoInd = line.find('echo')
        
        # Count normal decisions
        if opNameInd >= 0:
            if re.match(r''+re.escape(domain)+'|props-init|props-evaluate|props-do|props-sub-|props-build|props-return-condition|props-result',
                                           line[opNameInd+1:]):
                proc_dc += 1
            # Count simulated LTM retrieval time, separate from decision cycle time
            if re.match(r'props-query', line[opNameInd+1:]):
                ltmTimeBuff += LTM_TIME
                ltm_count += 1
            # Count simulated instruction element retrieval time, separate from decision cycle time
            if re.match(r'props-evaluate', line[opNameInd+1:]):
                ltmTimeBuff += PRIM_TIME
                prim_count += 1
            # Note successes as checkpoints
            if re.match(r''+re.escape(domain)+'|props-result-success', line[opNameInd+1:]):
                lastSuccessDC = proc_dc
                lastSuccessTime = ltmTimeBuff
            # Don't count failed-condition retrievals
            if NO_FAILS and re.match(r'props-reset', line[opNameInd+1:]):
                proc_dc = lastSuccessDC
                ltmTimeBuff = lastSuccessTime
        # Count new productions
        elif line.startswith("Learning new rule chunk"):
            p_count += 1
            if re.search(r'build-proposals', line[25:]):
                p_prp_count += 1
                p_type = 'prp'
            elif re.search(r'spread-result', line[25:]):
                p_cnd_count += 1
                p_type = 'cnd'
            elif re.search(r'success\*action', line[25:]):
                p_aut_count += 1
                p_type = 'aut'
            else:
                p_apl_count += 1
                p_type = 'apl'
        # Don't count chunks that were reverted to justifications due to lack of confidence
        elif line.startswith("Chunk confidence at") and not line.rstrip().endswith("Making chunk:"):
            p_count -= 1
            if p_type == 'prp':
                p_prp_count -= 1
            elif p_type == 'cnd':
                p_cnd_count -= 1
            elif p_type == 'aut':
                p_aut_count -= 1
            else:
                p_apl_count -= 1
        # Count invalid instruction retrievals
        elif line.startswith(" *** RETRACTING"):
            fail_count += 1
        #elif line.startswith("*** TASK SET TO "):
        #    tsk = line[16:]
        #    if tsk != lastProc:
        #        p_count = 0
        #    lastProc = tsk
        # Notice end of line instruction
        elif line.startswith("Say: "):
            if any(a in line[5:] for a in actions):
                # Calculate the appropriate fetching latencies
                if (actrfile):
                    PRIM_TIME = float(actrfile.readline().split()[-1])
                LTM_TIME = PRIM_TIME - (DC_TIME * 4.0)
                ltmTimeBuff = 0*LTM_TIME*ltm_count + PRIM_TIME*prim_count
                
                latencies += 0.3
                # Add data point - each corresponds to a completed procedure
                rows += [(proc_dc, p_count, sample, ltmTimeBuff, ltm_count+prim_count, fail_count, latencies, p_prp_count, p_apl_count, p_cnd_count, p_aut_count)]
                ltmTimeBuff = 0.0;
                ltm_count = 0
                prim_count = 0
                proc_dc = 0
                p_count = 0
                p_prp_count = 0
                p_apl_count = 0
                p_cnd_count = 0
                p_aut_count = 0
                fail_count = 0
                latencies = 0.0
                lastSuccessDC = 0
                lastSuccessTime = 0.0
            elif line[5:].startswith('read'):
                if any(a in line[10:] for a in ['limemax','limemin','toxinmax','toxinmin']):
                    latencies += 1.0
                else:
                    latencies += 0.3
        # Notice change in procedure
        elif echoInd >= 0 and len(rows) > 0:
            if not printing:
                printing = True
                f = open(outpath, "a")
            
            entry = rows[rowentry]
            ln = line[echoInd+5:].split()+[None, None, None, None, None, None]
            origlen = len(ln)
            tm = float(entry[0])*DC_TIME+entry[3]+SHIFT
            if ln[0] != lastProc:
                p_total = 0
                p_prp_total = 0
                p_apl_total = 0
                p_cnd_total = 0
                p_aut_total = 0
                latencies = 0.0
                lastProc = ln[0]
            p_total += entry[1]         # Add the count of learned chunks to the running total
            p_prp_total += entry[7]     # Add the count of learned proposal chunks to the running total
            p_apl_total += entry[8]     # Add the count of learned apply chunks to the running total
            p_cnd_total += entry[9]     # Add the count of learned condition chunks to the running total
            p_aut_total += entry[10]     # Add the count of learned auto chunks to the running total
            
            ln[origlen-6] = str(entry[0])                                               # DCs
            ln[origlen-5] = '{0:.4f}'.format(tm)                                        # DC time and LTM Time
            ln[origlen-4] = str(entry[4])                                               # LTM Count
            ln[origlen-3] = str(entry[5])                                               # Fail count
            ln[origlen-2] = str(p_total)                                               # Chunk count
            ln[origlen-1] = str(tm + entry[6])                              # Total time (adding action latencies)
            ln2 = ln[:3]+[ln[origlen-6], ln[origlen-2], ln[origlen-1], str(p_prp_total), str(p_apl_total), str(p_cnd_total), str(p_aut_total)]          # DCs, p_total, time, p_prp_total, p_apl_total, p_cnd_total, p_aut_total
            ln = DELIM.join(ln2)
            newline = ln + "\n"
            rowentry += 1
            f.write(newline)
        elif printing and line.startswith('***'):
            # Just finished procedure
            printing = False
            f.close()
            #print "Line: rows =", len(rows), ", rowentry =", rowentry  # Verify that record division are correct: these should be equal
            rows = []
            rowentry = 0
            #if lastProc != "a":
            #    p_count = 0
        
    infile.close()
    if timepath:
        actrfile.close()



def main():
    #pathdir = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/elio/results/'
    #pathdir = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/editors/results/'
    pathdir = '/media/bryan/My Book/festus_extended/Documents/Research/PRIMsDuplications/Elio/'
    conv_types = ['l12','l123','sc_lc12','sc_lc123']
    samples = ['']
    
    T = ['10']
    
    #domain = 'editors'
    #actions = ['next-instruction']
    #fetchTimeLatenciesFile = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/editors/editor_out2_X.dat"
    domain = 'elio'
    actions = ['enter']
    fetchTimeLatenciesFile = "/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/elio/elio-out8_X.dat"
    
    for ctype in conv_types:
        for t in T:
            for sample in samples:
                convertData(pathdir + 'verbose_' + domain + '_props_' + ctype + '_t' + t + sample + '.dat', 
                            fetchTimeLatenciesFile, domain, actions)
    

    
if __name__ == "__main__": main()
