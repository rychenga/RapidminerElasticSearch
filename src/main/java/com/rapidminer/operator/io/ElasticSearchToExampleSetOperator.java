package com.rapidminer.operator.io;

import java.net.InetAddress;
import java.util.logging.Logger;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import static org.elasticsearch.index.query.QueryBuilders.*;

import java.awt.List;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.collections15.IteratorUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Ontology;


public class ElasticSearchToExampleSetOperator extends AbstractReader<ExampleSet> {

	
	private static final Logger LOGGER = Logger.getLogger(ElasticSearchToExampleSetOperator.class
            .getName());

	public ElasticSearchToExampleSetOperator(OperatorDescription description)
		 {
		super(description, ExampleSet.class);
		// TODO Auto-generated constructor stub
	}


	@Override
	public ExampleSet read() throws OperatorException {
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		Attribute dateattribute = AttributeFactory.createAttribute("Text", Ontology.POLYNOMINAL);
		attributes.add(dateattribute);
		MemoryExampleTable table = new MemoryExampleTable(attributes);
		
		
		try
		{
		// TODO Auto-generated method stub
		//TODO figure out which is  the correct setting to use
		//TODO How does this work on cluster rater than one	
			
			
		Settings settings = Settings.settingsBuilder()
			       .put("cluster.name", "my-application").build();
		
		Client client = TransportClient.builder()
		        .settings(settings)
		        .build()
		        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
		
		
		LOGGER.finest("Done building client");
		
		QueryBuilder qb = termQuery("Text","event");
		
		LOGGER.finest("Query builder done");
		
		SearchResponse scrollResp = client.prepareSearch("twitter5", "twitter")
			//	.addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
		        .setScroll(new TimeValue(60000))
		        .addFields("Text")
		        .setQuery(qb)
		        .setSize(100).execute().actionGet();
		LOGGER.finest("scrollresp");
		
		//Add transport addresses and do something with the client...
		
		
		// MatchAll on the whole cluster with all default options
	    LOGGER.finest(String.valueOf(scrollResp.getHits().totalHits()));
	
	    //Scroll until no hits are returned
	    do {
	        for (SearchHit hit : scrollResp.getHits().getHits()) {
	            //Handle the hit...
	        	LOGGER.finest("searchhit");
		    	
	        	Set<Map.Entry<String, SearchHitField>> set = hit.getFields().entrySet();
	        	LOGGER.finest("Size is ");
	        	LOGGER.finest(String.valueOf(set.size()));
                Iterator<Map.Entry<String, SearchHitField>> iter = set.iterator();
                while (iter.hasNext()) {
                    SearchHitField field = iter.next().getValue();
                    LOGGER.finest(field.getValue().toString());
                    double[] values = new double[1];
					values[0] = attributes.get(0).getMapping().mapString(field.getValue().toString());
					table.addDataRow(new DoubleArrayDataRow(values));
                   
                }
	        }
	        scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
	    } while(scrollResp.getHits().getHits().length != 0); // Zero hits mark the end of the scroll and the while loop.
		
		}
		catch(Exception e)
		{
			LOGGER.finest(e.getMessage());
		}
		return table.createExampleSet();
	}

}
