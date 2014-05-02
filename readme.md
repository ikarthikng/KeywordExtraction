Keyword Extraction from the questions posted on stackoverflow

1. Design
The task at hand is to extract tags from the title and body given in the test document. Read the training file with all the records and separate the Id, title, body and tags. Use stop words technique to remove unwanted words and symbols from the text and form a set where one has the all the tags along with the frequency of their occurrence in the whole training set. Send this model as an input to the test file, where you follow the same procedure as for training file. Run stop words removal, count the frequency of the words which has occurred in the text and then test this frequency set with the training frequency set and test whether a word can belong to the tags category. This is nothing but probability calculation of a word which can be a keyword or not. Thus, for each record run the same procedure and write to a file the probable keywords along with their ID. This is the design I am following in the current procedure.

2. Implementation
1.	Read to csv file containing the data. I am using an open source tool called super csv reader to read the file at much faster rate and also I can clearly separate the ID, title, body and tags for further processing. 
2.	After separating the each field, I remove all the unwanted text using stop words. After cleaning the data I form a term frequency matrix of the words occurring in the title and body. Simultaneously, I have a model based on training set which has to be applied to the test set.
3.	Read the test file. Separate the ID, title, body. Apply stop words technique to the separated parameters and calculate the probability of each word occurring in the processed file. Applying the model got from training set will give you the probable keywords for the title and body of the training file.
4.	The top 5 words with best probability are considered as the keywords for that particular record. Correspondingly, form a file with ID and corresponding keywords.

3. Evaluation
1. The zip file contains the following items
•	KeywordExtraction.java
•	super-csv-2.1.0.jar
•	result.txt
2. For compiling the java file please use the following method
javac –cp super-csv-2.1.0.jar KeywordExtraction.java (windows)
javac -cp '/<full path>/super-csv-2.1.0.jar' KeywordExtraction.java
3. For running the program please use the following command
java -cp super-csv-2.1.0.jar;. KeywordExtraction Train3.csv Test3.csv
java -cp .:/<full path>/super-csv-2.1.0.jar KeywordExtraction Train3.csv Test3.csv
4. The result.txt is formed in the respective directory where KeywordExtraction.java file is present

Please go to the following link to read more about super csv reader

http://supercsv.sourceforge.net/index.html