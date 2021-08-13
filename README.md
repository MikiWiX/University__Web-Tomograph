# Web Tomograph
### University task
###### Java, Maven, Spring-Boot, JS, React, CSS
#### Running
In order to run React, open console, go to "/src/react/mymed" and type in:  
```
npm start
```
You will need to have npm installed on your PC.

To run the back-end, you need to have installed Java SE, JRE (I have been using Java 13, but it should go on slightly older versions as well),
and an IDE of your choice (created using Intelij IDEA Community, in case that matter).  
You also need to have Maven set-up with Spring-Boot depencies downloaded.  
You run this just as any other Spring-Boot app from basic tutorials, setup maven run configuration with this coommand line:  
```
spring-boot:run -f pom.xml
```
or this one if you need to debug (this will prevend runner from forking, and so debugger from disconnecting)  
```
spring-boot:run -Dspring-boot.run.fork=false -f pom.xml
```
You will also need to download additional libraries like DICOM Library for Java "dcm4che" in both version 2 and 3 (I got lost durning development due to lack of accessible information, and wanted to save time to focus on more important things)
#### Description
The task was to make a programm (in any common language) that simulates a primitive medical tomograph on discrete data.

It might not be the most efficient one, though ParallelStreams have been used for exercise and to increase the performance (and more importantly memory usage) considerably.
Because of the most simple filtering applied, output quality is definitely low. It was also part of task requirements - I had no time to do any more extensive research just on sinogram filtering.  
What I decided was that I would rather spend my time learning Maven, Spring-Boot and React - instead of making simple local programm I decided to go for an actual web app.
#### Side Notes
A side note for DICOM file generator: because of a wide variety od DICOM standards, and low accesibility to actual documents on new versions of it, as well as inconsistency between information across common sources, most modern DICOM readers won't support files generated here.

UI is in Polish Langage - because this has no real application other than as my teaching, I hadn't bothered translating it once it was done in Polish.

Here is a sample of how it looks like:
![Sample Screenshot](https://github.com/MikiWiX/University__Web-Tomograph/blob/main/Sample.png)
