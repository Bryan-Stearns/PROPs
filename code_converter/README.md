This program takes a source agent code file and converts the contents into a PROP agent code, the SMEM declarative instructions for that code, and/or a plain .soar agent equivalent to the same.
If given an Actransfer .lisp file, it will generate all of the other formats. This option requires a third input parameter, which determines the task name to use when naming the soar rules.
If given a .soar agent file, it will generate a .prop file and the corresponding SMEM instructions.
If given a .prop agent file, it will generate the corresponding SMEM instructions.

<pre>
Usage:
  source_file.lisp  new_file_name  task_name
  source_file.soar  new_file_name
  source_file.prop  new_file_name
</pre>
