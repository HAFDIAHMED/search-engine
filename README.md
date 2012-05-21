Search engine in Java
=====================
Developed for Information Retrieval course (ir12) at KTH.

Based on a existing skeleton by Johan Boye.

Running
-------
Using hashed index:

	$ java -cp bin SearchGUI -d texts/1000

Include PageRank in ranked search:

	$ java -cp bin SearchGUI -d texts/1000 -r texts/links/1000.txt

Using MegaMap (disk based index):

	$ java -cp bin:megamap SearchGUI -d texts/1000 -m
	$ java -cp bin:megamap SearchGUI -i index_name -m

Authors
-------
Victor Hallberg <<victorha@kth.se>><br>
Johan Stjernberg <<stjer@kth.se>>
