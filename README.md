Search engine in Java
=====================
Developed for Information Retrieval course (ir12) at KTH.

Running
-------
Using hashed index:

	$ java -cp bin;megamap;pdfbox SearchGUI -d svwiki\files\1000

Using MegaMap (disk based index):

	$ java -cp bin;megamap;pdfbox SearchGUI -d svwiki\files\1000 -m
	$ java -cp bin;megamap;pdfbox SearchGUI -i index_name -m

Authors
-------
Victor Hallberg <<victorha@kth.se>>
Johan Stjernberg <<stjer@kth.se>>
