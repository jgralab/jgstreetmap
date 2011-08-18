#! /bin/bash
if [ `uname` == "Darwin" ]; then
	java -Xmx2000M -cp build/jar/jgstreetmap.jar:../jgralab/build/jar/jgralab.jar:../common/lib/pcollections/pcollections-2.0.1.jar de.uni_koblenz.jgstreetmap.JGStreetMap -a $*
else
	java -Xmx2000M -cp build/jar/jgstreetmap.jar:../jgralab/build/jar/jgralab.jar de.uni_koblenz.jgstreetmap.JGStreetMap $*
fi

