package tlt.rest;
 
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import tlt.JSONobj.JSONClassList;
import tlt.queryapp.QueryApp;
 
@Path("/getCourses")
public class RESTful {
 
	@javax.ws.rs.core.Context
	ServletContext context;
	//http://localhost:12020/burimsblackboardintegration-backend/rest/getCourses/mkousheh
	@GET
	@Path("/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public JSONClassList getMsg(@PathParam("param") String username) {
		/* Missing Inputs, going to replace this with SpringSource Application Context */
		JSONClassList output = null;
		QueryApp application = (QueryApp)context.getAttribute("Queryapp");
		try{
			output = application.sendCourseQuery(username);
		}
		catch(Exception e){	
			return new JSONClassList();
		}
 
		return output;
 
	}
 
}