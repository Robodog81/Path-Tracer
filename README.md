# Java Path-Tracer
## **A simple Java Path-tracer for rendering cool scenes written in a single java class.**


![A simple scene](DemoImage.png?raw=true)


I have done most of the development of this program in VS Code but it was originally made in Bluej. You should be able to ignore all of the redundant files.

**How to Run**
* Download the .jar from the beta releases section
* Make sure you have a java JDK installed
* Double click the file (on Windows) or run "java -jar PathTracer.jar" with the terminal in the location of the file (seems to work better for linux)

**How to Customize**
* Download the source code
* Extract the zip
* Open the extracted folder in VS Code
* Install the java extension pack for VS Code to run and compile java.
* Run the Main.java class to launch the program.

At the top of the program there are a bunch of finals that effect the various parts of the render engine. You can play around with these to change performance and other aspects.

**Editing The Scene:**
If you scroll down a bit you will see a section with a bunch of addSphere() functions being called. You can change these or add more to edit the scene. Keep in mind that if you add more spheres you must change the SPHEREAMT final to match the new amount.

**Features**
* Multi Threading
* Matte materials using lambertian scattering
* Reflective Metal Surfaces with variable fuzziness
* Performance metric display
* Running average accumulation
* Optional tone mapping

**How It Works:**
 Every pixel on the screen sends out a ray that bounces around and gathers information about the environment. When finished it will return a color that is an proximate of the final colour. The program repeats this for many layers, each one may bounce the ray in a different direction giving it more information of the scene. The image that is rendered is an average of every layer that was calculated. You can see the amount of layers in the top left corner along with other metrics such as FPS.

For more information you can google it or look at the book that i constantly referenced to build this project:
https://raytracing.github.io/books/RayTracingInOneWeekend.html
