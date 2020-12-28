Projog - an implementation of the Prolog logic programming language for the Java platform.
http://www.projog.org/

* Documentation:

To access the manual please visit: http://www.projog.org/manual.html

* Launching the console:

You need to have the JDK installed and present in the PATH in order to launch the projog console.

To launch the projog console, from the command line, "cd" to the unzipped directory and run the "projog-console.sh" or "projog-console.bat" script.

You can optionally provide names of files, containing Prolog syntax, as arguments to "projog-console.sh"/"projog-console.bat" and they will be interpreted when the console starts.

e.g.:

projog-bin> java -version
java version "1.8.0_172"
Java(TM) SE Runtime Environment (build 1.8.0_172-b11)
Java HotSpot(TM) 64-Bit Server VM (build 25.172-b11, mixed mode)

projog-bin> projog-console.sh towers-of-hanoi-example.pl

INFO Reading prolog source in: projog-bootstrap.pl from classpath
Projog Console
projog.org
INFO Reading prolog source in: towers-of-hanoi-example.pl from file system

?- hanoi(2).
[move,a,disc,from,the,left,pole,to,the,right,pole]
[move,a,disc,from,the,left,pole,to,the,centre,pole]
[move,a,disc,from,the,right,pole,to,the,centre,pole]

yes (0 ms)

no (0 ms)

?- W=X, X=1+1, Y is W, Z is -W.
W = 1 + 1
X = 1 + 1
Y = 2
Z = -2

yes (0 ms)

?- quit.
