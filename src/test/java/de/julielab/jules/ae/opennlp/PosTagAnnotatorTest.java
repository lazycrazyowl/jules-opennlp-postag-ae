/** 
 * OpenNLPPOSTaggerAnnotatorTest.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: buyko
 * 
 * Current version: 1.1
 * Since version:   1.0
 *
 * Creation date: 08.12.2006 
 * 
 * //TODO insert short description
 **/

package de.julielab.jules.ae.opennlp;

import java.util.Iterator;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import de.julielab.jules.types.PennBioIEPOSTag;
import de.julielab.jules.types.Sentence;
import de.julielab.jules.types.Token;
import junit.framework.TestCase;

public class PosTagAnnotatorTest extends TestCase {

	String text = "A study on the Prethcamide";
	String postags = "hm;NN;IN;DT;NN;";

      
	public void initCas(JCas jcas) {
		
		jcas.reset();
		jcas.setDocumentText("A study on the Prethcamide");
		Sentence s1 = new Sentence(jcas);
		s1.setBegin(0);
		s1.setEnd(text.length());
		s1.addToIndexes();
		Token t1 = new Token(jcas);
		t1.setBegin(0); t1.setEnd(1); t1.addToIndexes();
		PennBioIEPOSTag pos = new PennBioIEPOSTag(jcas);
		pos.setValue("hm");
		pos.addToIndexes();
		FSArray postags = new FSArray(jcas, 10);
		postags.set(0, pos);
		postags.addToIndexes();
		t1.setPosTag(postags);		
		Token t2 = new Token(jcas);
		t2.setBegin(2); t2.setEnd(7);t2.addToIndexes();
		Token t3 = new Token(jcas); 
		t3.setBegin(7); t3.setEnd(10);t3.addToIndexes();
		Token t4 = new Token(jcas); 
		t4.setBegin(11); t4.setEnd(14);t4.addToIndexes();
		Token t5 = new Token(jcas); 
		t5.setBegin(15); t5.setEnd(26);t5.addToIndexes();
		
	}
	
	
	public void testProcess() {

		boolean annotationsOK = true;

		XMLInputSource posXML = null;
		ResourceSpecifier posSpec = null;
		AnalysisEngine posAnnotator = null;

		try {
			posXML = new XMLInputSource(
					"src/test/resources/PosTagAnnotatorTest.xml");
			posSpec = UIMAFramework.getXMLParser().parseResourceSpecifier(
					posXML);
			posAnnotator = UIMAFramework
					.produceAnalysisEngine(posSpec);
		} catch (Exception e) {
			e.printStackTrace();
		}


			JCas jcas = null;
			try {
				jcas = posAnnotator.newJCas();
			} catch (ResourceInitializationException e) {
				e.printStackTrace();
			}

			// get test cas with sentence annotation
			initCas(jcas);
			
			try {
				posAnnotator.process(jcas, null);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// get the offsets of the sentences
			JFSIndexRepository indexes = jcas.getJFSIndexRepository();
			Iterator tokIter = indexes.getAnnotationIndex(Token.type)
					.iterator();

			String predictedPOSTags = "";
			
			
			while (tokIter.hasNext()) {
				Token t = (Token) tokIter.next();
				
				PennBioIEPOSTag tag = (PennBioIEPOSTag) t.getPosTag().get(0);
				
				predictedPOSTags = predictedPOSTags+ tag.getValue() + ";";			
				
			}
			System.out.println("Wanted:" + postags + "\n Predicted" + predictedPOSTags);

			// compare offsets
			if (!predictedPOSTags.equals(postags)) {
				annotationsOK = false;
			}


		assertTrue(annotationsOK);

	}
	
	
}
