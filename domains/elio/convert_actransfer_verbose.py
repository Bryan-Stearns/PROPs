# -*- coding: utf-8 -*-
"""
Created on Wed Mar  8 09:38:37 2017

@author: bryan
"""

def convertData(path):
    printing = False
    lastProc = ""
    DELIM = "\t"
    
    sample = 0
    rowentry = 0
    proc_dc = 0
    #proc_rt = 0
    p_count = 0
    rs_count = 0    # Retrievals
    avg_rs_time = 0.0   # Avg time for retrievals
    prev_rs_time = 0.0  # Total time at step before the last retrieval
    
    rows = []

    outpath = path[:-4] + "_X.dat"
    infile = open(path)
    
    for line in infile:
        temp = line.split()
        # Count normal decisions
        if line.startswith("PRODUCTION-FIRED", 36):
            proc_dc += 1
        # Count new productions
        elif line.startswith("(P "):
            p_count += 1
        # Count retrievals
        elif len(temp) >= 3 and temp[2] == "RETRIEVED-CHUNK":
            rs_count += 1
            avg_rs_time += float(temp[0]) - prev_rs_time
            prev_rs_time = float(temp[0])  # Though I don't think there would ever be two retrievals in a row
        # Measure start time before retrievals (retrievals are always preceded by a PROCEDURAL step)
        elif len(temp) >= 3 and temp[1] == "PROCEDURAL":
            prev_rs_time = float(temp[0])
        # Notice end of procedure
        #elif line.startswith("------                 BREAK-EVENT Stopped by !stop!", 13):
        # Notice end of line instruction
        elif line.startswith("ACTION: ENTER", 14):
            if rs_count != 0:
                avg_rs_time = avg_rs_time / rs_count
            # Add data point - each corresponds to a completed procedure
            rows += [(proc_dc, p_count, sample, rs_count, avg_rs_time)]
            #p_count = 0
            proc_dc = 0
            rs_count = 0
            avg_rs_time = 0.0
            prev_rs_time = 0.0
        # Notice change in procedure
        elif line[:10].upper() == "PROCEDURE-" and len(rows) > 0:
            if not printing:
                printing = True
                f = open(outpath, "a")
                lastProc = line[10]
                if lastProc == 'D':
                    sample += 1
            
            entry = rows[rowentry]
            ln = "\t".join(line[:-1].replace("Resetting module imaginalreq", "").split())
            newline = ln + DELIM + str(entry[0]) + DELIM + str(entry[1]) + DELIM + str(entry[2]) + DELIM + str(entry[3]) + DELIM + '{0:.4f}'.format(entry[4]) + "\n"
            rowentry += 1
            f.write(newline)
        elif printing:
            # Just finished procedure
            printing = False
            f.close()
            rows = []
            rowentry = 0
            if lastProc != "a":
                p_count = 0
        
    infile.close()



def main():
    #pathdir = 'C:\cygwin64\home\Bryan\'
    #pathdir = '/home/bryan/Dropbox/UM_misc/Soar/Research/'
    pathdir = '/home/bryan/Actransfer/supplemental/Actransfer distribution/Elio/MyResults/'
    convertData(pathdir + 'elio-out8.txt')
    
    convertData(pathdir + 'elio-doA-out.txt')
    convertData(pathdir + 'elio-doB-out.txt')
    convertData(pathdir + 'elio-doC-out.txt')
    convertData(pathdir + 'elio-doD-out.txt')
    

    
if __name__ == "__main__": main()    
