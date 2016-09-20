# Overview
The main aim of this project is to get the best parsing for a given query. This basically means recognizing different phrases within the query.
We need some kind of training data to generate these phrases.
The way this project works is:

1. Generate all possible parsings for the given query (would have an exponential complexity which is reduced later #See OptimizationsAndWorking).
2. For each possible parsing, a naive-bayes like score is calculated. 
3. The main scoring is done by going through all the documents in the training set and finding the probability of bunch of words occurring together as a phrase as compared to them occurring randomly in the same document. Then the score is normalized.
Some higher importance is given to the title field as compared to content field which is configurable.
4. Finally after scoring each of the possible parsing, the one with the highest score is returned.

# SetUp
To avoid the exponential complexity and get quick query time responses, some optimizations are carried out.We need to index the data in a specific way for this to occur.

1. While indexing the documents, we assume that we have two fields, "title" and "content". All the data that you need to learn on must be copied to these fields(or make new fields and copy it.)
2. For optimizations, we need create new fieldtype named "unigrams", "bigrams" ,"trigrams", which would basically contain the shingleFilter with it. For these field types, fields like unigrams_title,unigrams_content etc are made.
Example:
Change schema.xml to add these fields.
```xml
<field name="unigrams" type="unigram" indexed="true" stored="false" required="false" multiValued="true" />
<field name="bigrams" type="bigram" indexed="true" stored="false" required="false" multiValued="true" />
<field name="trigrams" type="trigram" indexed="true" stored="false" required="false" multiValued="true"/>
<field name="unigrams_title" type="unigram" indexed="true" stored="false" required="false" multiValued="true" />
<field name="bigrams_title" type="bigram" indexed="true" stored="false" required="false" multiValued="true" />
<field name="trigrams_title" type="trigram" indexed="true" stored="false" required="false" multiValued="true"/> (edited)

<fieldType name="unigram" class="solr.TextField" positionIncrementGap="100">
  <analyzer>
    <filter class="solr.ASCIIFoldingFilterFactory"/>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="solr.LowerCaseFilterFactory" />
  </analyzer>
</fieldType>

<fieldType name="bigram" class="solr.TextField" positionIncrementGap="100">
  <analyzer>
    <filter class="solr.ASCIIFoldingFilterFactory"/>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="solr.LowerCaseFilterFactory" />
    <filter class="solr.ShingleFilterFactory" minShingleSize="2" maxShingleSize="2" outputUnigrams="false"/>
  </analyzer>
</fieldType>

<fieldType name="trigram" class="solr.TextField" positionIncrementGap="100">
  <analyzer>
    <filter class="solr.ASCIIFoldingFilterFactory"/>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="solr.LowerCaseFilterFactory" />
    <filter class="solr.ShingleFilterFactory" minShingleSize="3" maxShingleSize="3" outputUnigrams="false"/>
  </analyzer>
</fieldType>
```

#Usage
Adding the requestHandler to SolrConfig.xml:
``` xml
<requestHandler name="/yourCustomName" class="ProbabilisticQueryParserRequestHandler">
```
To use this handler we would simply make a request using this handler.
Eg:
http://localhost:8983/solr/collectionName/yourCustomName?indent=on&q=q=san%20francisco&wt=json

Given the right data, we would expect the output to be "san francisco" which is a phrase now.

To get the parsed query from your program, you need to get the "ParsedQuery" param.

#Optimizations And Working

1. For the given query, we find all the unigrams,bigrams and trigrams of the query. For each of these ngrams, we find their occurance from the fields we have indexed(unigrams_content,bigrams_content,trigrams_content etc). Because of indexing these fields, it becomes a simple TermQuery which is much faster that using phraseQuery for the same cause.
We build a map from these counts and then pass in to generate partitions.
2. Step 1 helps to reduce number of partitions of the query that are passed for scoring in the first place by removing those partitions which have sub-paritions that have 0 occurance.
3. Now these maps also help for scoring as we can do a simple lookups from these occurance maps for the caluclations in our scoring.

