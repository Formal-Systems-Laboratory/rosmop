package rosmop.parser.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * A RVM input file, which contains one preamble and at least one specification.
 * @author A. Cody Schuffelen
 */
public class MonitorFile {
    
    private String preamble;
    private List<Specification> specifications;
    
    public MonitorFile() {
		this.specifications = new ArrayList<Specification>();
	}
    
    /**
     * Construct a MonitorFile out of the preamble and some specifications.
     * @param preamble Declarations at the beginning of the file, e.g. imports.
     */
    public MonitorFile(String preamble, List<Specification> specifications) {
    	this.preamble = preamble;
        this.specifications = specifications;
        
        addMsgTypeIncludes();
    }
    
    private void addMsgTypeIncludes() {
    	for (Specification specification : specifications) {
			for (Event event : specification.getEvents()) {
				addPreamble("#include \"" + event.getMsgType() + ".h\"");
			}
		}
	}

	/**
     * Language-specific declarations that go at the top of the file, e.g. includes, imports.
     * @return Language-specific top of the file declarations.
     */
    public String getPreamble() {
        return preamble;
    }
    
    public void addPreamble(String includeDeclarations) {
		this.preamble += "\n" + includeDeclarations;
	}
    
    /**
     * An unmodifiable list of the specifications in the monitoring file.
     * @return A list of the specifications.
     */
    public List<Specification> getSpecifications() {
        return specifications;
    }
    
    public void addSpecifications(List<Specification> specifications) {
		this.specifications.addAll(specifications);
	}

	public void setPreamble(String preamble) {
		this.preamble = preamble;
	}

	public void setSpecifications(List<Specification> specifications) {
		this.specifications = specifications;
	}
    
}