package org.rhok.pta.donate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.Arrays;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 
 * @author Ishmael Makitla
 *         GDG Pretoria, RHoK Pretoria
 *         2013
 *         South Africa
 *
 */
@SuppressWarnings("serial")
public class UserID extends HttpServlet{
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)throws IOException {
		String payload = req.getParameter("payload");			
		try {
			if(payload !=null){
			    String decodedPayload = URLDecoder.decode(payload,"UTF-8");
				 doRequest(resp,decodedPayload);				
			}
			else{
				//look for it in the Htt-Content
				getRawPayload(req,resp);
			}
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			writeOutput(resp," Error: There were issues procesin your donation request");
		}
	}
	/**
	 * 
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)throws IOException {
		
		//get the parameters of the offer -JSON
		String payload = req.getParameter("payload");			
		try {
			if(payload !=null){
			    String decodedPayload = URLDecoder.decode(payload,"UTF-8");
				 doRequest(resp,decodedPayload);				
			}
			else{
				//look for it in the Htt-Content
				getRawPayload(req,resp);
			}
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			writeOutput(resp," Error: There were issues procesin your donation request");
		}
	}
	
	/**
	 * 
	 * @param request
	 * @param response
	 */
	private void getRawPayload(HttpServletRequest request, HttpServletResponse response){
		StringBuffer rawData = new StringBuffer();
		  String line = null;
		  try {
			  	BufferedReader reader = request.getReader();
			  	while ((line = reader.readLine()) != null){
			  		rawData.append(line);
			  	}
			  
			  	System.out.println("getRawPayload(...) DATA = \n"+rawData);
			  		
		  } catch (Exception e) { e.printStackTrace(); }

		  if(rawData.length()>0){
			  doRequest(response, rawData.toString()); 
		  }
		  else{
			  System.err.println("The data stream is empty - no data received");
		  }
	}
	/**
	 * 
	 * @param response
	 * @param data - JSON document used for authentication.
	 */
	private void doRequest( HttpServletResponse response, String data){
		LoginRequest loginRequest = (new Gson()).fromJson(data, LoginRequest.class);
		if(loginRequest != null){
			Entity knownUser = getUser(loginRequest.getUsername(), loginRequest.getPassword());
			Object uidProperty =  knownUser.getProperty("");
			String userID = (uidProperty != null?  uidProperty.toString(): "");
			if(!userID.trim().isEmpty()){
				writeOutput(response, asServerResponse(DonateMyStuffConstants.DONATION_OFFER_SUCCESS, userID));
			}
			else{
				//user not found or login error
				writeOutput(response, asServerResponse(DonateMyStuffConstants.DONATION_OFFER_FAILURE, userID));
			}
			
		}
	}
	
	/**
	 * This method queries the DataStore for an Entity that matches the combination of the username and password
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	private Entity getUser(String username, String password){
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		Query query = new Query("RegistrationRequest");
		
		//filter is same as the WHERE CLAUSE
		Filter userNameFilter = new Query.FilterPredicate("username", FilterOperator.EQUAL, username);
		Filter passwordNameFilter = new Query.FilterPredicate("password", FilterOperator.EQUAL, password);
		
		query.setFilter(userNameFilter);
		
		CompositeFilter userpasswordCombinationFilter = new CompositeFilter(CompositeFilterOperator.AND, Arrays.asList(userNameFilter, passwordNameFilter));
		
		query.setFilter(userpasswordCombinationFilter);
		
		PreparedQuery pq = datastore.prepare(query);
		Entity user = pq.asSingleEntity();
		
		return user;
	}
	
	/**
	 * 
	 * @param status
	 * @param message
	 * @return
	 */
	private String asServerResponse(int status, String message){
		String response = "";
		JsonObject responseJSON = new JsonObject();
		responseJSON.addProperty("status", status);
		responseJSON.addProperty("message", message);
		response = responseJSON.toString();
		return response;
	}

	
	/**
	 * This method is used to write the output (JSON)
	 * @param response - response object of the incoming HTTP request
	 * @param output - message to be out-put
	 */
	private void writeOutput(HttpServletResponse response,String output){
		//send back JSON response
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try{
        	Writer outputWriter = response.getWriter();
        	outputWriter.write(output);
        }
        catch(IOException ioe){
        	
        }
	}
}
