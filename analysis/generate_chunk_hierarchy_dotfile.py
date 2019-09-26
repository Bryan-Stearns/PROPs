# -*- coding: utf-8 -*-
"""
Created on Thu Sep 26 13:20:28 2019

This code takes a text file of an agent's generated chunks (from "print -fc") and creates a .gv file for visualizing the learned operator hierarchy.

@author: bryan
"""

import re

def getOpHierarchy(path):
    retList = []
    infile = open(path)     # Read a file of fully-printed chunks (from the "print -fc" command in Soar)
    
    # Scan for "chunk*apply*props*build-proposals*return*..." chunks, which define the hierarchy
    while True:
        line = infile.readline()
        
        if line.startswith("sp {chunk*apply*props*build-proposals*return*"):
            # Find the composition info within the chunk
            while True:
                line = infile.readline()
                match = re.search(r"(\(concat) (\S+) (\S+) (\S+)(\))", line)
                if match:
                    # Break apart the compositional tokens
                    part1 = match.group(3).strip("|")
                    part2 = match.group(4).strip("|")
                    combo = "-"+part1+part2
                    retList.append([(part1),(part2),(combo)])
                    break
        
        if not line:
            break
    infile.close()
    
    return retList
    
def makeDotFile(data, outpath):
    outfile = open(outpath, 'w')
    rankset = set()
    
    # Write the dot header
    outfile.write("digraph G {\n\trankdir=BT ranksep=1.0\n")
    # Write each edge
    for triple in data: # Format: (source1, source2, combo)  Print as: "source1 -> combo \n source2 -> combo"
        outfile.write('\t"'+triple[0]+'" -> "'+triple[2]+'";\n\t"'+triple[1]+'" -> "'+triple[2]+'";\n')
        # Collect primitives to mark them as sharing the base rank in the graph
        if triple[0][0] == "_":
            rankset.add(triple[0])
        if triple[1][0] == "_":
            rankset.add(triple[1])
    # Write the rank formatting commands
    outfile.write('\n\t{ rank = same; ')
    for prop in rankset:
        outfile.write('"'+prop+'"; ')
    outfile.write(' }\n')
        
    # Write the dot closer
    outfile.write("}")
    
    outfile.close()

def main():
    path = '/home/bryan/Documents/GitHub_Bryan-Stearns/PROPs/domains/elio/'
    
    data = getOpHierarchy(path+"elio_agent_all_chunks.soar")
    makeDotFile(data, path+"elio_procedure_hierarchy.gv")

    
if __name__ == "__main__": 
    main()
