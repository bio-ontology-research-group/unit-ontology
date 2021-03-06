@Grab(group='net.sourceforge.owlapi', module='owlapi-distribution', version='3.5.7')

import org.semanticweb.owlapi.io.* 
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.apibinding.OWLManager
import uk.ac.ebi.owlapi.extension.*

def infile = new File(args[0])
def patouri = "http://purl.obolibrary.org/obo/pato.owl"
def patouri2 = "http://purl.obolibrary.org/obo/"

def l = []

def start = false

def term = false
def exp = null

infile.eachLine {
  if (it.startsWith("[Term]")) {
    term = true
    start = true
  }
  if (!start) {
  } else {
    if (it.startsWith("[Term]")) {
      start = true
      term = true
      if (exp!=null) { l << exp }
      exp = new Expando()
      exp.isa = []
      exp.rel = []
      exp.syn = []
      exp.unit = false
      exp.prefix = false
    } else if (it.trim().size()==0) {
      term = false
    } else if (it.trim().startsWith("id:")) {
      exp.id = it.substring(3).trim()
    } else if (it.trim().startsWith("def:")) {
      exp.definition = it.substring(4).trim()
    } else if (it.trim().startsWith("subset: unit_group_slim")) {
      exp.unit = false
    } else if (it.trim().startsWith("subset: unit_slim")) {
      exp.unit = true
    } else if (it.trim().startsWith("subset: prefix_slim")) {
      exp.prefix = true
    } else if (it.trim().startsWith("relationship: is_unit_of")) {
      def rel = it.substring(25).trim()
      if (rel.indexOf('!')>-1) {
	rel = rel.substring(0,rel.indexOf('!')).trim()
      }
      exp.rel << rel
    } else if (it.trim().startsWith("name:")) {
      exp.name = it.trim().substring(6)
    } else if (it.trim().startsWith("is_a:")) {
      exp.isa << it.substring(6,it.indexOf('!')).trim()
    } else if (it.trim().startsWith("synonym:")) {
      exp.syn << [
        (it.trim() =~ /"(.*)"/)[0][1],
        it.trim().tokenize(' ')[2]
      ]
    }
  }
}
if (exp!=null) { l << exp }

def prefixmap = [:]
l.each { ex ->
  if (ex.prefix) {
    prefixmap[ex.name] = ex.id
  }
}

def onturi = "http://purl.obolibrary.org/obo/"
OWLOntologyManager man = OWLManager.createOWLOntologyManager();
OWLDataFactory fac = man.getOWLDataFactory()
OWLOntology ont = man.createOntology(IRI.create(onturi+"uo.owl")) // full version
OWLOntology ont2 = man.createOntology(IRI.create(onturi+"uo2.owl")) // without instances
OWLOntology ont3 = man.createOntology(IRI.create(onturi+"uo3.owl")) // without singleton defs
OWLOntology ont4 = man.createOntology(IRI.create(onturi+"uo4.owl")) // without units as classes
OWLOntology ont5 = man.createOntology(IRI.create(onturi+"uo5.owl")) // without PATO references
def unitof = fac.getOWLObjectProperty(IRI.create(onturi+"is_unit_of"))
def hasprefix = fac.getOWLObjectProperty(IRI.create(onturi+"has_prefix"))

def exactSynProp = fac.getOWLAnnotationProperty(IRI.create("oboInOwl:hasExactSynonym"))
def narrowSynProp = fac.getOWLAnnotationProperty(IRI.create("oboInOwl:hasNarrowSynonym"))
def relatedSynProp = fac.getOWLAnnotationProperty(IRI.create("oboInOwl:hasRelatedSynonym"))

PrintWriter oboout = new PrintWriter(new BufferedWriter(new FileWriter(new File("unit-xp.obo"))))

def prefixdone = new TreeSet()
l.each {
  def ex = it

  def cls = onturi+(it.id.replaceAll(":","_"))
  def cl = fac.getOWLClass(IRI.create(cls))
  man.addAxiom(ont, fac.getOWLDeclarationAxiom(cl))
  man.addAxiom(ont2, fac.getOWLDeclarationAxiom(cl))
  man.addAxiom(ont3, fac.getOWLDeclarationAxiom(cl))
  man.addAxiom(ont5, fac.getOWLDeclarationAxiom(cl))
  if (!it.unit) {
    man.addAxiom(ont4, fac.getOWLDeclarationAxiom(cl))
  }

  def setprefix = false
  def prefixid = null
  def baseid = null
  if (ex.unit) {
    prefixmap.keySet().each { prefix ->
      if (ex.name.startsWith(prefix)) {
	def basename = ex.name.replaceAll(prefix,"").trim()
	l.each { ex2 ->
	  if (ex2.unit && ex2.name.trim() == basename) {
	    def baseunit = ex2.id.replaceAll(":0", ":1")
	    def unprefixedunit = ex2.id
	    oboout.println("[Term]")
	    oboout.println("id: "+ex.id)
	    oboout.println("intersection_of: "+baseunit+" ! "+ex2.name)
	    oboout.println("intersection_of: has_prefix "+prefixmap[prefix]+" ! "+prefix)
	    oboout.println("")
	    if (! (baseunit in prefixdone)) {
	      prefixdone.add(baseunit)
	      oboout.println("[Term]")
	      oboout.println("id: "+baseunit)
	      oboout.println("name: "+basename+" based unit")
	      ex2.isa.each {
		if (it.indexOf("0000045")==-1) {
		  oboout.println("is_a: "+it)
		}
	      }
	      def cl0 = fac.getOWLClass(IRI.create(onturi+(baseunit.replaceAll(":","_"))))
	      man.addAxiom(ont, fac.getOWLDeclarationAxiom(cl0))
	      man.addAxiom(ont2, fac.getOWLDeclarationAxiom(cl0))
	      man.addAxiom(ont3, fac.getOWLDeclarationAxiom(cl0))
	      man.addAxiom(ont5, fac.getOWLDeclarationAxiom(cl0))

	      def label = fac.getRDFSLabel()
	      def definition = fac.getRDFSComment()
	      def anno = fac.getOWLAnnotation(label, fac.getOWLTypedLiteral(basename+" based unit"))
	      def annoassert = fac.getOWLAnnotationAssertionAxiom(IRI.create(onturi+(baseunit.replaceAll(":","_"))),anno)
	      man.addAxiom(ont,annoassert)
	      man.addAxiom(ont2,annoassert)
	      man.addAxiom(ont3,annoassert)
	      man.addAxiom(ont5,annoassert)
	      ex2.isa.each {
		if (it.indexOf("0000045")==-1) {
		  def cl1 = fac.getOWLClass(IRI.create(onturi+it.replaceAll(":","_")))
		  def subcax = fac.getOWLSubClassOfAxiom(cl0,cl1)
		  man.addAxiom(ont, subcax)
		  man.addAxiom(ont2, subcax)
		  man.addAxiom(ont3, subcax)
		  man.addAxiom(ont5, subcax)
		}
	      }
	      def origcl = fac.getOWLClass(IRI.create(onturi+(ex2.id.replaceAll(":","_"))))
	      def subcax = fac.getOWLSubClassOfAxiom(origcl,cl0)
	      man.addAxiom(ont, subcax)
	      man.addAxiom(ont2, subcax)
	      man.addAxiom(ont3, subcax)
	      man.addAxiom(ont5, subcax)
	      
	      oboout.println("")
	      oboout.println("[Term]")
	      oboout.println("id: "+ex2.id)
	      oboout.println("name: "+ex2.name)
	      oboout.println("is_a: "+baseunit)
	      oboout.println("")
	    }
	    setprefix = true
	    prefixid = prefixmap[prefix]
	    baseid = baseunit
	  }
	}
      } else { // does not start with a prefix: still create the grouping classes
	def withprefix = false
	prefixmap.keySet().each { p ->
	  if (ex.name.startsWith(p)) {
	    withprefix=true
	  }
	}
	if (!withprefix)  {
	  def basename = ex.name.trim()
	  def baseunit = ex.id.replaceAll(":0", ":1")
	  def cl0 = fac.getOWLClass(IRI.create(onturi+(baseunit.replaceAll(":","_"))))
	  man.addAxiom(ont, fac.getOWLDeclarationAxiom(cl0))
	  man.addAxiom(ont2, fac.getOWLDeclarationAxiom(cl0))
	  man.addAxiom(ont3, fac.getOWLDeclarationAxiom(cl0))
	  man.addAxiom(ont5, fac.getOWLDeclarationAxiom(cl0))
	  def label = fac.getRDFSLabel()
	  def definition = fac.getRDFSComment()
	  def anno = fac.getOWLAnnotation(label, fac.getOWLTypedLiteral(basename+" based unit"))
	  def annoassert = fac.getOWLAnnotationAssertionAxiom(IRI.create(onturi+(baseunit.replaceAll(":","_"))),anno)
	  man.addAxiom(ont,annoassert)
	  man.addAxiom(ont2,annoassert)
	  man.addAxiom(ont3,annoassert)
	  man.addAxiom(ont5,annoassert)
	  ex.isa.each {
	    if (it.indexOf("0000045")==-1) {
	      def cl1 = fac.getOWLClass(IRI.create(onturi+it.replaceAll(":","_")))
	      def subcax = fac.getOWLSubClassOfAxiom(cl0,cl1)
	      man.addAxiom(ont, subcax)
	      man.addAxiom(ont2, subcax)
	      man.addAxiom(ont3, subcax)
	      man.addAxiom(ont5, subcax)
	    }
	  }
	  def origcl = fac.getOWLClass(IRI.create(onturi+(ex.id.replaceAll(":","_"))))
	  def subcax = fac.getOWLSubClassOfAxiom(origcl,cl0)
	  man.addAxiom(ont, subcax)
	  man.addAxiom(ont2, subcax)
	  man.addAxiom(ont3, subcax)
	  man.addAxiom(ont5, subcax)
	}
      }
    }
  }

  if (setprefix) {
    def baseclass = fac.getOWLClass(IRI.create(onturi+(baseid.replaceAll(":","_"))))
    def prefixclass = fac.getOWLClass(IRI.create(onturi+(prefixid.replaceAll(":","_"))))
    def unitax = fac.getOWLEquivalentClassesAxiom(cl, fac.getOWLObjectIntersectionOf(
						    baseclass, fac.getOWLObjectSomeValuesFrom(
						      hasprefix, prefixclass)))
    man.addAxiom(ont, unitax)
    man.addAxiom(ont2, unitax)
    man.addAxiom(ont3, unitax)
    man.addAxiom(ont5, unitax)
						  
  }
  if (it.unit) {
    //    def cls2 = onturi+"i"+it.id
    def cls2 = onturi+(it.id.replaceAll(":","_"))
    def ind = fac.getOWLNamedIndividual(IRI.create(cls2))
    def oneof = fac.getOWLObjectOneOf(ind)
    def equiv = fac.getOWLEquivalentClassesAxiom(cl,oneof)
    man.addAxiom(ont, equiv)
    equiv = fac.getOWLClassAssertionAxiom(cl, ind)  
    man.addAxiom(ont3, equiv)
    man.addAxiom(ont5, equiv)
}
  it.isa.each { sup ->
    sup = sup.replaceAll(":","_")
    def cl2 = fac.getOWLClass(IRI.create(onturi+sup))
    def subc = fac.getOWLSubClassOfAxiom(cl,cl2)
    man.addAxiom(ont, subc)
    man.addAxiom(ont2, subc)
    man.addAxiom(ont3, subc)
    man.addAxiom(ont5, subc)
    def cls2 = onturi+(it.id.replaceAll(":","_"))
    def ind = fac.getOWLNamedIndividual(IRI.create(cls2))
    def equiv = fac.getOWLClassAssertionAxiom(cl2, ind)
    man.addAxiom(ont4, equiv)
  }

  // add the label
  // TODO why do only some of hte entries get this, wtf?
  def label = fac.getRDFSLabel()
  def definition = fac.getRDFSComment()
  def anno = fac.getOWLAnnotation(label, fac.getOWLTypedLiteral(it.name))
  def annoassert = fac.getOWLAnnotationAssertionAxiom(IRI.create(cls),anno)
  man.addAxiom(ont,annoassert)
  man.addAxiom(ont5,annoassert)

  // add synonyms
  it.syn.each { s ->
    def aProp
    switch(s[1]) {
      case 'RELATED':
        aProp = relatedSynProp; break;
      case 'NARROW':
        aProp = narrowSynProp; break;
      case 'EXACT': default: 
        aProp = exactSynProp
    }

    def sAnn = fac.getOWLAnnotation(aProp, fac.getOWLTypedLiteral(s[0]))
    def sAnnAss = fac.getOWLAnnotationAssertionAxiom(IRI.create(cls), sAnn)
    man.addAxiom(ont, sAnnAss)
    man.addAxiom(ont5, sAnnAss)
  }

  // Add the definition...
  if (it.definition!=null) {
    anno = fac.getOWLAnnotation(definition, fac.getOWLTypedLiteral(it.definition))
    annoassert = fac.getOWLAnnotationAssertionAxiom(IRI.create(cls),anno)
    man.addAxiom(ont,annoassert)
    man.addAxiom(ont2,annoassert)
    man.addAxiom(ont3,annoassert)
    man.addAxiom(ont4,annoassert)
    man.addAxiom(ont5,annoassert)
  }
  if (it.rel.size()>0) {
    it.rel = it.rel.collect { fac.getOWLClass(IRI.create(patouri2+it.replaceAll(":","_"))) }
    def se = new TreeSet()
    it.rel.each { se << it }
    def cl2 = fac.getOWLObjectUnionOf(se)
    cl2 = fac.getOWLObjectAllValuesFrom(unitof, cl2)
    def subc = fac.getOWLSubClassOfAxiom(cl,cl2)
    man.addAxiom(ont, subc)
    man.addAxiom(ont2, subc)
    man.addAxiom(ont3, subc)
    man.addAxiom(ont4, subc)
  }
}

File f = new File("uo.owl")
man.saveOntology(ont, IRI.create("file:"+f.getCanonicalFile()))
f = new File("uo-without-instances.owl")
man.saveOntology(ont2, IRI.create("file:"+f.getCanonicalFile()))
f = new File("uo-without-singleton-definitions.owl")
man.saveOntology(ont3, IRI.create("file:"+f.getCanonicalFile()))
f = new File("uo-without-units-as-classes.owl")
man.saveOntology(ont4, IRI.create("file:"+f.getCanonicalFile()))
f = new File("uo-without-pato-references.owl")
man.saveOntology(ont5, IRI.create("file:"+f.getCanonicalFile()))

f = new File("uo-with-pato.owl")
def imp = fac.getOWLImportsDeclaration(IRI.create("http://purl.obolibrary.org/obo/pato.owl"))
AddImport ai = new AddImport(ont, imp)
man.applyChange(ai)
man.saveOntology(ont, IRI.create("file:"+f.getCanonicalFile()))

oboout.flush()
oboout.close()
