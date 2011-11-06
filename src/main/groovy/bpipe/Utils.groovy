/*
 * Copyright (c) 2011 MCRI, authors
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package bpipe

/**
 * Miscellaneous internal utilities used by Gruffus.
 * 
 * @author ssadedin@mcri.edu.au
 */
class Utils {
	/**
	 * Check either a single file passed as a string or a list
	 * of files passed as a collection
	 * @param f
	 * @return
	 */
	static checkFiles(def f, type="input") {
		
		if(f instanceof String)
			if(!new File(f).exists())
				throw new PipelineError("Expected $type file $f could not be found")
		else
		if(f instanceof Collection)
			f.each {
				if(!new File(it).exists())
					throw new PipelineError("Expected $type file $it could not be found")
			}
	}
	
	/**
	 * Return true iff all outputs are newer than all inputs
	 * @param outputs 	a single string or collection of strings
	 * @param inputs	a single string or collection of strings
	 * @return
	 */
	static boolean isNewer(def outputs, def inputs) {
        
        // Some pipeline stages don't expect any outputs
        if(outputs == null)
            return false
		
		// Box into a collection for simplicity
        outputs = box(outputs)
	
		outputs.collect { new File(it) }.every { f ->
			
//            println "Check $f"
			if(!f.exists()) {
				return false
			}
                
			if(inputs instanceof String || inputs instanceof GString) {
	            if(f.name == inputs)
	                return true
                else {
					return (new File(inputs).lastModified() <= f.lastModified()) 
                }
			}
			else
			if(isContainer(inputs)) {
				return !inputs.collect { new File(it) }.any { 
					// println "Check $it : " + it.lastModified() + " >  " + "$f : " + f.lastModified() 
					it.lastModified() > f.lastModified() 
				}
			}
			else 
				throw new PipelineError("Don't know how to interpret $inputs of type " + inputs.class.name)
				
			return false	
		}
	}
	
    /**
     * Attempt to delete all of the specified outputs, if any
     * 
     * @param outputs   string or collection of strings representing 
     *                  names of files to be deleted
     */
	static void cleanup(def outputs) {
		if(!outputs)
			return
			
		if(!(outputs instanceof Collection))
			outputs = [outputs]
		
		outputs.collect { new File(it) }.each { File f -> if(f.exists()) {  
            // it.delete() 
            File trashDir = new File(".bpipe/trash")
            if(!trashDir.exists())
                trashDir.mkdirs()
                
            File dest = new File(trashDir, f.name)
                
            if(!Runner.opts.t) {
	            println "Cleaning up file $f to $dest" 
	            f.renameTo(dest)
            }
            else
	            println "[TEST MODE] Would clean up file $f to $dest" 
         }}
	}
    
    /**
     * Return true if the specified object is a collection or
     * array, false otherwise.
     */
    static boolean isContainer(def obj) {
        return (obj != null) && (obj instanceof Collection || obj.class.isArray())
    }
    
    /**
     * Normalize a single input and array into a collection, 
     * return existing collections as is
     */
    static Collection box(outputs) {
        
        if(outputs == null)
            return []
        
        if(outputs instanceof Collection)
	        return outputs
        
        if(outputs.class.isArray())    
            return outputs as List
            
        return [outputs]
    }
    
    /**
     * Return the given inputs as an individual object if they are 
     * a collection with only 1 entry, otherwise just return the object
     */
    static unbox(inputs) {
        return isContainer(inputs) && inputs.size() == 1 ? inputs[0] : inputs
    }
    
    static first(inputs) {
        if(inputs == null)
	        return null
            
        if(isContainer(inputs))
            return inputs.size() > 0 ? inputs[0] : null
            
        // Plain object
        return inputs
    }
    
    /**
     * Check if any of the specified inputs are wrapped in PipelineInput and if so, unwrap them
     * 
     * @param inputs    a single object or array or collection of objects
     */
    static unwrap(inputs) {
        def result = unbox(box(inputs).collect { it instanceof PipelineInput?it.input:it })
        return result
    }
}