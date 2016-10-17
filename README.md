# The CS 227 Dream Killer
### About
This program was designed to catch students cheating in Iowa State University's CS 227. It uses MinHash as a measure to estimate Jaccard similarity between assignments. This program was designed to quickly catch very trivial acts of cheating with a low error rate. It is not effective at catching anything more involved than changing things such as variable names or whitespace. It's pretty quick, taking under a minute to cluster a four file submission turned in by 320 students.

### Limitations
This was quickly hacked together and has some definite issues. The main one is that in runs in quadratic time relative to the number of students! There are two places causing this but this limitation but both can be fixed in the future. These two places are determining the average and standard deviation for maximum similarities among documents as well as the clustering process itself. 

The clustering process itself can be optimized using locality sensitive hashing but this is dependant on chosing a good band size which is itself dependant on knowing the correct similarity threshold. An optimization could be made to estimate the average and standard deviation using an iterative algorithm instead of an exact measure. 



### Usage
The user should specify the target directory and submission files to cluster as command line arguments. For example:

``` java -jar dreamkiller.jar /Volumes/cs227ta/submissions/hw2 PaymentMachine.java ExitMachine.java ParkingRateUtil.java``` 