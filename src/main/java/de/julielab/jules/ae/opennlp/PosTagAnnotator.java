/** 
 * OpennlpPosTagger.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: buyko
 * 
 * Current version: 2.0
 * Since version:   1.0
 *
 * Creation date: 30.01.2008
 * 
 * Analysis Engine that invokes the OpenNLP POS Tagger. This annotator assumes that
 * sentences and tokens have been annotated in the CAS. We iterate over sentences, 
 * then iterate over tokens in the current sentece to accumulate a list of tokens, then invoke the
 * OpenNLP POS Tagger on the list of tokens. 
 **/

package de.julielab.jules.ae.opennlp;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.lang.english.PosTagger;
import opennlp.tools.ngram.Dictionary;
import opennlp.tools.postag.POSDictionary;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.types.POSTag;
import de.julielab.jules.types.Sentence;
import de.julielab.jules.types.Token;

public class PosTagAnnotator extends JCasAnnotator_ImplBase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PosTagAnnotator.class);

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
	 * true if a tag dictionary should be used, please consider data fields
	 * tagdict, caseSensitive
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
	throws ResourceInitializationException {

		super.initialize(aContext);

		try {

			LOGGER
			.info("[OpenNLP POSTag Annotator]initializing OpenNLP POSTag Annotator ...");
			// Get configuration parameter values
			String modelFile = (String) aContext
			.getConfigParameterValue("modelFile");
			postagset = (String) aContext.getConfigParameterValue("tagset");
			useTagdict = (Boolean) aContext
			.getConfigParameterValue("useTagdict");
			tagdict = (String) aContext.getConfigParameterValue("tagDict");
			caseSensitive = (Boolean) aContext
			.getConfigParameterValue("caseSensitive");

			// Get OpenNLP POS Tagger, initialize with a model
			if (useTagdict)
				tagger = new PosTagger(modelFile, (Dictionary) null,
						new POSDictionary(tagdict, caseSensitive));
			else
				tagger = new PosTagger(modelFile, (Dictionary) null);
		} catch (Exception e) {
			LOGGER
			.error("[OpenNLP POStag Annotator] Could not load Part-of-speech model: "
					+ e.getMessage());
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas aJCas) {

		LOGGER.info("[OpenNLP POSTag Annotator]  processing document ...");
		ArrayList<Token> tokenList = new ArrayList<Token>();
		ArrayList<String> tokenTextList = new ArrayList<String>();

		AnnotationIndex sentenceIndex = (AnnotationIndex) aJCas
		.getJFSIndexRepository().getAnnotationIndex(Sentence.type);
		AnnotationIndex tokenIndex = (AnnotationIndex) aJCas
		.getJFSIndexRepository().getAnnotationIndex(Token.type);

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
						Class[] parameterTypes = new Class[] { JCas.class };
						Class myNewClass = Class.forName(postagset);
						Constructor myConstructor = myNewClass.getConstructor(parameterTypes);
						pos = (POSTag) myConstructor.newInstance(aJCas);
						pos.setBegin(token.getBegin());
						pos.setEnd(token.getEnd());
						pos.setValue(posTag);
						pos.setComponentId(COMPONENT_ID);
						pos.addToIndexes();

					} catch (SecurityException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]"
								+ e1.getMessage());
					} catch (IllegalArgumentException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]"
								+ e1.getMessage());
					} catch (ClassNotFoundException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]"
								+ e1.getMessage());
					} catch (NoSuchMethodException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]"
								+ e1.getMessage());
					} catch (InstantiationException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]"
								+ e1.getMessage());
					} catch (IllegalAccessException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]"
								+ e1.getMessage());
					} catch (InvocationTargetException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]"
								+ e1.getMessage());
					}

					FSArray postags = token.getPosTag();
					if (postags == null) {
						postags = new FSArray(aJCas, 1);
						try {
							postags.set(0, pos);
						} catch (CASRuntimeException e) {
							LOGGER.error("[OpenNLP POSTag Annotator]" + e.getMessage());
						}				
						token.setPosTag(postags);
					}
					else {
						int numPosTags = postags.size();
						int lastElementIndex = numPosTags - 1;
						if (postags.get(lastElementIndex) != null) {
							FSArray extendedPosTags = new FSArray(aJCas, numPosTags + 1);
							extendedPosTags.copyFromArray(postags.toArray(), 0, 0, numPosTags);
							extendedPosTags.set(numPosTags, pos);
							postags = extendedPosTags; 
						}
						else {
							while ((lastElementIndex > 0) && (postags.get(lastElementIndex - 1) == null)) {
								lastElementIndex--;
							}
							postags.set(lastElementIndex, pos);
						}
					}

				}
			} catch (CASRuntimeException e) {
				LOGGER.error("[OpenNLP POSTag Annotator]" + e.getMessage());
				LOGGER
				.error("[OpenNLP POSTag Annotator]  list of tags shorter than list of words");
			}
		}
	}

}