/** 
 * OpennlpPosTagger.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: buyko
 * 
 * Current version: 1.0	
 * Since version:   1.0
 *
 * Creation date: 30.11.2006 
 * 
 * Analysis Engine that invokes the OpenNLP POS tagger. This annotator assumes that
 * sentences and tokens have been annotated in the CAS. We iterate over sentences, 
 * then iterate over tokens in the current sentece to accumulate a list of tokens, then invoke the
 * OpenNLP POS tagger on the list of tokens. 
 **/

package de.julielab.jules.ae.opennlp;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.lang.english.PosTagger;
import opennlp.tools.ngram.Dictionary;
import opennlp.tools.postag.POSDictionary;

import com.ibm.uima.UimaContext;
import com.ibm.uima.analysis_component.JCasAnnotator_ImplBase;
import com.ibm.uima.cas.CASRuntimeException;
import com.ibm.uima.cas.FSIterator;
import com.ibm.uima.cas.text.AnnotationIndex;
import com.ibm.uima.jcas.cas.FSArray;
import com.ibm.uima.jcas.impl.JCas;
import com.ibm.uima.resource.ResourceInitializationException;

import de.julielab.jules.types.GeniaPOSTag;
import de.julielab.jules.types.POSTag;
import de.julielab.jules.types.PennBioIEPOSTag;
import de.julielab.jules.types.PennPOSTag;
import de.julielab.jules.types.Sentence;
import de.julielab.jules.types.Token;
import de.julielab.jules.utility.AnnotationTools;
import de.julielab.jules.utility.JulesTools;

public class PosTagAnnotator extends JCasAnnotator_ImplBase {
	
	/**
	 * component Id
	 */
    private static final String COMPONENT_ID = "de.julielab.jules.ae.OpenNLPPosTagger";
    
    /**
     * instance of the Opennlp POS Tagger
     */
    private PosTagger tagger;
    
    /**
     * the used postagset 
     */
    private String postagset;
    
    /**
     * the language of the text of analysis
     */
    private String language;
    
    /**
     * true if a tag dictionary should be used, please consider data fields tagdict, caseSensitive
     */
    private Boolean useTagdict;
    
    /**
     * Path to tag Dictionary
     */
    private String tagdict;
    
    /**
     * Is Tag Dictionary Use Case senstive
     */
    private Boolean caseSensitive;
    
	@Override
    public void initialize(UimaContext aContext)
            throws ResourceInitializationException{
		
        	super.initialize(aContext);

        try {
        	
        	System.out.println("initializing OpenNLP POSTag Annotator ...");
            // Get configuration parameter values
            String modelFile = (String) aContext.getConfigParameterValue("modelFile");
            postagset = (String) aContext.getConfigParameterValue("tagset");
            language = (String)aContext.getConfigParameterValue("language");
            useTagdict = (Boolean)aContext.getConfigParameterValue("useTagdict");
            tagdict = (String)aContext.getConfigParameterValue("tagDict");
            caseSensitive = (Boolean)aContext.getConfigParameterValue("caseSensitive");
            
            //Get OpenNLP POS Tagger, initialize with a model
            if(useTagdict)
            	tagger = new PosTagger(System.getProperty("user.dir") + modelFile, (Dictionary)null, new POSDictionary(System.getProperty("user.dir") + tagdict,caseSensitive));
            else 
            	tagger = new PosTagger(System.getProperty("user.dir") + modelFile, (Dictionary)null);
        } catch (Exception e) {
            throw new ResourceInitializationException(e);
        }
    }

	@Override
    public void process(JCas aJCas) {
		
		
		System.out.println("OpenNLP POSTag Annotator:  processing next document ...");
        ArrayList<Token> tokenList = new ArrayList<Token>();
        ArrayList<String> tokenTextList = new ArrayList<String>();

        AnnotationIndex sentenceIndex = (AnnotationIndex) aJCas.getJFSIndexRepository().getAnnotationIndex(Sentence.type);
        AnnotationIndex tokenIndex = (AnnotationIndex) aJCas.getJFSIndexRepository().getAnnotationIndex(Token.type);

        // iterate over Sentences
        FSIterator sentenceIterator = sentenceIndex.iterator();
        while (sentenceIterator.hasNext()) {
        	 tokenList.clear();
             tokenTextList.clear();
        	 Sentence sentence = (Sentence) sentenceIterator.next();            
            
        	 // iterate over Tokens
             FSIterator tokenIterator = tokenIndex.subiterator(sentence);
             while (tokenIterator.hasNext()) {
                Token token = (Token) tokenIterator.next();
                tokenList.add(token);
                tokenTextList.add(token.getCoveredText());
            }
           
            List tokenTagList = tagger.tag(tokenTextList);

            try {
                for (int i = 0; i < tokenList.size(); i++) {
                    Token token = (Token) tokenList.get(i);
                    String posTag = (String) tokenTagList.get(i);
                    POSTag pos = null;
                    
                    try {
						pos = (POSTag) AnnotationTools.getAnnotationObject(aJCas, postagset);
						pos.setBegin(token.getBegin()); pos.setEnd(token.getEnd());
						Class c = pos.getClass();         
	                	if (c.equals(PennPOSTag.class))
	                		((PennPOSTag)pos).setValue(posTag);
	                	if (c.equals(PennBioIEPOSTag.class))
	                		((PennBioIEPOSTag)pos).setValue(posTag);
	                	if (c.equals(GeniaPOSTag.class))
	                		((GeniaPOSTag)pos).setValue(posTag);
	                	

	                    pos.setComponentId(COMPONENT_ID);
	                    pos.setLanguage(language);
	                    pos.addToIndexes();
	                    
	                	
					} catch (SecurityException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IllegalArgumentException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (ClassNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (NoSuchMethodException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (InstantiationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IllegalAccessException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (InvocationTargetException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
           
                    
              
                    FSArray postags;
                    if (token.getPosTag()==null){                    	
                    	postags = new FSArray (aJCas, JulesTools.DEFAULT_ADDITION_SIZE);
                    	try {
							postags = JulesTools.addToFSArray(postags, pos);
						} catch (CASRuntimeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    	postags.addToIndexes();
                    	token.setPosTag(postags);
                    	
                
                    }
                   
                    else {                    	
                    	postags = JulesTools.addToFSArray(token.getPosTag(), pos);               
               	
                    }
                    
                   
                }
            } catch (CASRuntimeException e) {
            	System.out.println(e.getMessage());
            	System.err.println("POS tagger error - list of tags shorter than list of words");
            }
        }
    }

	

}