#! /bin/bash

java -Xmx1500M -cp build/jar/jgstreetmap.jar:../jgralab/build/jar/jgralab.jar de.uni_koblenz.jgstreetmap.importer.OsmImporter $1
