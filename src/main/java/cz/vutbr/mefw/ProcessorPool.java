package cz.vutbr.mefw;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *
 */

public class ProcessorPool {


    Map<String, ProcessorAdapter> processors;
    Config config;

    public ProcessorPool(Config config){
        processors = new HashMap<String, ProcessorAdapter>();
        this.config = config;
    }


    /** Getter which returns processor adapter instance by name
     * @param processorName name of the processor adapter class
     * @return processor adapter instance
     */
    public ProcessorAdapter get(String processorName){
        return this.processors.get(processorName);
    }

    /** Reads classes names from folder and return as array.
     * @param stripExtension is switch which controls whether list of classes will have extensions or not
     * @return list of classes available for loading
     */
    public List<String> getProcessorList(boolean stripExtension){
        ArrayList<String> classes = new ArrayList<String>();
        System.out.println(Paths.get(this.config.get("pluginsPath")).toAbsolutePath().toString());
        File folder = new File(Paths.get(this.config.get("pluginsPath")).toAbsolutePath().toString());
        File[] listOfFiles = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".class");
            }
        });
        if(listOfFiles!=null){
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    if(stripExtension){
                        classes.add(this.stripExtension(listOfFiles[i].getName()));
                    }else{
                        classes.add(listOfFiles[i].getName());
                    }
                }
            }

        }
        return classes;
    }

    /** Load selected class by name
     * @param classname is name of the class which should be loaded
     */
    public void load(String classname){
        this.processors.put(classname, this.loadClass(classname));
    }

    /** Load all classes from predefined folder
     */
    public void loadAll(){
       List<String> classes = this.getProcessorList(true);
       for (int i = 0; i < classes.size(); i++) {
            this.processors.put(classes.get(i), this.loadClass(classes.get(i)));
       }
    }

    /** Dynamicly loads class from specified folder and create its instance
     * @param name is the name of the class which should be loaded
     * @return instance of the loaded class or null
     */
    private ProcessorAdapter loadClass(String name){

        try {
            //ClassLoader classLoader = this.getClass().getClassLoader();
            URL url = new URL("file:/" + Paths.get(this.config.get("projectPath")).toAbsolutePath().toString());
            URL[] urls = new URL[]{url};
            ClassLoader classLoader = new URLClassLoader(urls);

            Class loadedMyClass = classLoader.loadClass("cz.vutbr.mefw.plugins." + name);
            System.out.println("Loading processor: " + loadedMyClass.getName());

            // Create a new instance from the loaded class
            Constructor constructor = loadedMyClass.getConstructor();
            ProcessorAdapter myClassObject = (ProcessorAdapter)constructor.newInstance(this.config);
            System.out.println("Initializing processors data: " + loadedMyClass.getName());
            myClassObject.load();
            return myClassObject;

        //TODO: proper error handling
        } catch (ClassNotFoundException e) {
            System.err.println("Could not find processor: "+name);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error when loading: "+name);
            e.printStackTrace();
        }

        return null;
    }

    /** String extension from filename
     * @param str filename string
     * @return filename without extension
     */
    static String stripExtension (String str) {
        if (str == null) return null;
        int pos = str.lastIndexOf(".");
        if (pos == -1) return str;
        return str.substring(0, pos);
    }

    /** Return all loaded processors as HashMap
     * @return HashMap with pairs ProcessorName=ProcessorAdapterInstance
     */
    public Map<String, ProcessorAdapter> getAll() {
        return this.processors;
    }
}
