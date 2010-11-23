#! /bin/bash

java -Xmx4G -cp build/jar/jgstreetmap.jar:../jgralab/build/jar/jgralab.jar de.uni_koblenz.jgstreetmap.importer.OsmImporter $*
