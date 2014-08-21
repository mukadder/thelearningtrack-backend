package tlt.SOAPclientHelper;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.rampart.handler.WSSHandlerConstants;
import org.apache.rampart.handler.config.OutflowConfiguration;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.handler.WSHandlerConstants;

import tlt.JSONobj.JSONStudent;
import tlt.JSONobj.JSONStudentList;
import tlt.JSONobj.JSONgrades;
import tlt.WSDLstub.ContextWSStub;
import tlt.WSDLstub.ContextWSStub.LogoutResponse;
import tlt.WSDLstub.CourseWSStub;
import tlt.WSDLstub.GradebookWSStub;
import tlt.WSDLstub.UserWSStub;
import tlt.WSDLstub.ContextWSStub.CourseIdVO;
import tlt.WSDLstub.ContextWSStub.GetMemberships;
import tlt.WSDLstub.ContextWSStub.GetMembershipsResponse;
import tlt.WSDLstub.ContextWSStub.LoginTool;
import tlt.WSDLstub.CourseWSStub.CourseFilter;
import tlt.WSDLstub.CourseWSStub.CourseVO;
import tlt.WSDLstub.CourseWSStub.GetCourse;
import tlt.WSDLstub.CourseWSStub.GetCourseResponse;
import tlt.WSDLstub.GradebookWSStub.GetGrades;
import tlt.WSDLstub.GradebookWSStub.GetGradesResponse;
import tlt.WSDLstub.GradebookWSStub.ScoreFilter;
import tlt.WSDLstub.GradebookWSStub.ScoreVO;
import tlt.WSDLstub.UserWSStub.GetUser;
import tlt.WSDLstub.UserWSStub.GetUserResponse;
import tlt.WSDLstub.UserWSStub.UserFilter;
import tlt.WSDLstub.UserWSStub.UserVO;

@SuppressWarnings("deprecation")
public class SOAPhandler {

	private String blackboardServerURL;
	private String sharedSecret;
	private String vendorId;
	private String clientProgramId;
	private ConfigurationContext ctx;
	private ContextWSStub contextWSStub;
	private ServiceClient client;
	private Options options;
	private OutflowConfiguration ofc;
	private PasswordCallbackClass pwcb;

	public SOAPhandler(String modulePath, String blackboardServerURL,
			String sharedSecret, String vendorId, String clientProgramId) throws AxisFault{
		this.blackboardServerURL = blackboardServerURL;
		this.sharedSecret = sharedSecret;
		this.vendorId = vendorId;
		this.clientProgramId = clientProgramId;
		this.ctx = ConfigurationContextFactory.createConfigurationContextFromFileSystem(modulePath);
		this.contextWSStub = null;
		this.client = null;
		this.options = null;
		this.ofc = null;
		this.pwcb = null;
	}

	public ContextWSStub getContextWSStub() {
		return contextWSStub;
	}

	public void setContextWSStub(ContextWSStub contextWSStub) {
		this.contextWSStub = contextWSStub;
	}

	public List<String> courseQuery(String username){
		return null;
	}

	public boolean loginTool() throws RemoteException{

		/*=== STEP 1 - Create object of the Web Service we need to use for the operation ====*/
		/* Create an object of the ContextWSStub.  This class
		 * was generated by using Axis2 and the Context web service
		 * WSDL file.  After generating the file it was copied into 
		 * this project - see package edu.ku.it.si.bbcontextws.generated.
		 * The Context web service is used to get a session id value
		 * from the Blackboard installation and then login using the
		 * the proxy tool.
		 */
		contextWSStub = new ContextWSStub(ctx,
				"http://" + blackboardServerURL + "/webapps/ws/services/Context.WS");
		client = contextWSStub._getServiceClient();
		options = client.getOptions();
		options.setProperty(HTTPConstants.HTTP_PROTOCOL_VERSION,
				HTTPConstants.HEADER_PROTOCOL_10);
		/*===================================================================================*/





		/*============== STEP 2:  Setup the Web Service - Security settings ==============*/

		/* Next, setup ws-security settings and specify the CallBackHandler class */
		pwcb = new PasswordCallbackClass();
		options.setProperty(WSHandlerConstants.PW_CALLBACK_REF, pwcb);

		/* Must use deprecated class of setting up security because
		 * the SOAP response doesn't include a security header.  Using
		 * the deprecated OutflowConfiguration class we can specify
		 * that the security  header is only for the outgoing SOAP
		 * message.
		 */
		ofc = new OutflowConfiguration();
		ofc.setActionItems("UsernameToken Timestamp");
		ofc.setUser("session");

		ofc.setPasswordType("PasswordText");
		options.setProperty(WSSHandlerConstants.OUTFLOW_SECURITY, ofc.getProperty());
		client.engageModule("rampart");
		/*===================================================================================*/




		/*====== STEP 3: Get the session ID from Blackboard for using this web service ======*/

		/* call initialize method of the Context web service to get the sessionid */
		String sessionValue = contextWSStub.initialize().get_return();

		/* set the sessionid on the callback handler so it is used by all subsequent webservice calls. */
		pwcb.setSessionId(sessionValue);

		/*===================================================================================*/


		/* ==STEP 4:  Login to our Blackboard installation using the information for a proxy tool ==
		 * that is authorized. Create a LoginTool object that has information needed 
		 * to authorize this session to use the web services.  In this example, we are 
		 * using the proxy tool that was previously setup by another program and approved 
		 * to access the Blackboard web services classes and methods 
		 */
		LoginTool loginArgs = new LoginTool();
		loginArgs.setPassword( sharedSecret );

		loginArgs.setClientVendorId( vendorId );
		loginArgs.setClientProgramId( clientProgramId );
		loginArgs.setLoginExtraInfo( "" );
		loginArgs.setExpectedLifeSeconds( 60*60 );

		//Call the Context Web Services login method 
		//so that the session id is authorized to access
		//web services 
		return contextWSStub.loginTool(loginArgs).get_return();
	}
	
	
	


	/**
	 * This method will take in a username for input and find all the courseIDs for all the courses
	 * that the user is registered for
	 * @param username
	 * @return an array of courseIDs for a specific user
	 * @throws RemoteException
	 */
	public String[] getCoursesID(String username) throws RemoteException{
		/* STEP 6 - Use a specific method of the Web Service to perform the operation we need for this application */


		/* use the Context web service to get the Blackboard course memberships for the provided
		 * Blackboard username
		 * Create a GetMemberships object and specify the username to get the course memberships for
		 */
		GetMemberships getMemberships = new GetMemberships();
		getMemberships.setUserid(username);
		GetMembershipsResponse getMembershipsResponse = contextWSStub.getMemberships(getMemberships);
		/*===============================================================================================*/


		/* STEP 7 - Process the web service response from our Blackboard installation 
		 * Get the return from calling the web service method
		 * which is an Array of CourseIDVO objects
		 */
		CourseIdVO [] courseIdVOs = getMembershipsResponse.get_return() ;

		/*
		 *Create an Array of String objects where each String
		 *is a Course id value
		 */

		String [] courseIds = new String[courseIdVOs.length];

		int i = 0;

		for (CourseIdVO courseIdVO : courseIdVOs) {
			courseIds[i] = courseIdVO.getExternalId();
			i++;
		}
		return courseIds;
	}

	public List<String> getCourseNames(String[] courseIds,String username) throws RemoteException{
		/*
		 * Create a GetCourse object that is used
		 * to specify how we want the Course web
		 * service to filter the Blackboard courses
		 * when we call the Course web service's getCourse
		 * method.
		 */
		GetCourse getCourse = new GetCourse();

		CourseFilter courseFilter = new CourseFilter();

		//Filter type 3 is for the id value of the 
		//course which is the PK1 column value in 
		//the course_main table
		courseFilter.setFilterType(3);

		courseFilter.setIds(courseIds);

		getCourse.setFilter(courseFilter);
		/*===============================================================*/

		/*
		 * STEP 8 - Create an object of another Blackboard web service to perform the next operation
		 * we need for this application
		 */

		/*
		 * Use the Course web service classes to get the
		 * Course value object for each course ID 
		 * stored in the courseIds array.
		 */
		CourseWSStub courseWSStub = new CourseWSStub(ctx,
				"http://" + blackboardServerURL + "/webapps/ws/services/Course.WS");

		client = courseWSStub._getServiceClient();

		options = client.getOptions();

		options.setProperty(HTTPConstants.HTTP_PROTOCOL_VERSION,
				HTTPConstants.HEADER_PROTOCOL_10);
		/*==============================================================================*/


		/*
		 * STEP 9 - Setup the WS-Security for the request to this web service
		 * NOTE that we will re-use the same callback handler (with its session ID)
		 * as the previous web service request used.
		 */

		// Next, setup ws-security settings
		// Reuse the same callback handler
		options.setProperty(WSHandlerConstants.PW_CALLBACK_REF, pwcb);
		ofc = new OutflowConfiguration();
		ofc.setActionItems("UsernameToken Timestamp");
		ofc.setUser("session");

		ofc.setPasswordType("PasswordText");
		options.setProperty(WSSHandlerConstants.OUTFLOW_SECURITY, ofc
				.getProperty());
		client.engageModule("rampart");
		/*==============================================================================*/


		/* ========== STEP 10 - make the request to this web service ============*/

		GetCourseResponse getCourseResponse = courseWSStub.getCourse(getCourse);
		/*=========================================================================*/




		/* =====STEP 11 - process the response from this web service	=========== */
		CourseVO [] courseVOs = getCourseResponse.get_return() ;
		List<String> courseTitles = new ArrayList<String>();

		for (CourseVO courseVO : courseVOs) {

			courseTitles.add( courseVO.getName() );

		}
		/*==========================================================================*/

		return courseTitles;
	}

	public JSONStudentList getUsersInfoforCourse(String courseID) throws RemoteException{

		/* Test on obtaining users in a course from Blackboard */
		/* Create a GetUser object and create a ScodreFilter to find scores by course IDs*/
		UserFilter userFilter = new UserFilter();
		userFilter.setFilterType(4);
		userFilter.setCourseId(new String[]{courseID});
		GetUser getUser = new GetUser();
		getUser.setFilter(userFilter);

		/* Use the User web service classes to get the user value object for each course ID 
		 * stored in the courseIds array.*/
		UserWSStub userWSSstub = new UserWSStub(ctx,
				"http://" + blackboardServerURL + "/webapps/ws/services/User.WS");
		client = userWSSstub._getServiceClient();
		options = client.getOptions();
		options.setProperty(HTTPConstants.HTTP_PROTOCOL_VERSION,
				HTTPConstants.HEADER_PROTOCOL_10);

		/* Setup the WS-Security for the request to this web service
		 * NOTE: that we will re-use the same callback handler (with its session ID)
		 *  as the previous web service request used.
		 */

		// Next, setup ws-security settings & Reuse the same callback handler
		options.setProperty(WSHandlerConstants.PW_CALLBACK_REF, pwcb);
		ofc = new OutflowConfiguration();
		ofc.setActionItems("UsernameToken Timestamp");
		ofc.setUser("session");

		ofc.setPasswordType("PasswordText");
		options.setProperty(WSSHandlerConstants.OUTFLOW_SECURITY, ofc
				.getProperty());
		client.engageModule("rampart");

		/* Make the request to this web service	 */
		GetUserResponse getUserRespone = userWSSstub.getUser(getUser);

		/* Process the response from this web service */
		UserVO[] userVOs = getUserRespone.get_return();

		
		JSONStudentList studentList = new JSONStudentList();
		for (UserVO userVO : userVOs) 
			if(userVO.getId()!=null)
				studentList.getStudentList().add(new JSONStudent(userVO.getExtendedInfo().getFamilyName(),
					userVO.getExtendedInfo().getGivenName(), userVO.getId()));

		return studentList;
	}


	public List<JSONgrades> getUserGrades(String userID,String courseID) throws RemoteException{
		/* Test on obtaining grades from Blackboard */
		/* Create a GetGrades object and create a ScoreFilter to find scores by course IDs*/
		GetGrades getGrades = new GetGrades();
		ScoreFilter filter = new ScoreFilter();
		filter.setFilterType(8);
		filter.setUserIds(new String[]{userID});
		getGrades.setFilter(filter);
		getGrades.setCourseId(courseID);

		/* Use the Gradebook web service classes to get the Gradebook value object for each course ID 
		 * stored in the courseIds array.*/
		GradebookWSStub gradebookWSSstub = new GradebookWSStub(ctx,
				"http://" + blackboardServerURL + "/webapps/ws/services/Gradebook.WS");
		client = gradebookWSSstub._getServiceClient();
		options = client.getOptions();
		options.setProperty(HTTPConstants.HTTP_PROTOCOL_VERSION,
				HTTPConstants.HEADER_PROTOCOL_10);

		/* Setup the WS-Security for the request to this web service
		 * NOTE: that we will re-use the same callback handler (with its session ID)
		 *  as the previous web service request used.
		 */

		// Next, setup ws-security settings & Reuse the same callback handler
		options.setProperty(WSHandlerConstants.PW_CALLBACK_REF, pwcb);
		ofc = new OutflowConfiguration();
		ofc.setActionItems("UsernameToken Timestamp");
		ofc.setUser("session");

		ofc.setPasswordType("PasswordText");
		options.setProperty(WSSHandlerConstants.OUTFLOW_SECURITY, ofc
				.getProperty());
		client.engageModule("rampart");

		/* Make the request to this web service	 */
		GetGradesResponse getGradesRespone = gradebookWSSstub.getGrades(getGrades);

		/* Process the response from this web service */
		ScoreVO[] scoreVOs = getGradesRespone.get_return();


		List<JSONgrades> grades = new ArrayList<JSONgrades>();
		
		
		/* Print out the Information from the ScoreVOs */
		for (ScoreVO scoreVO : scoreVOs) {
			grades.add(new JSONgrades(scoreVO.getColumnId(),0, 0, scoreVO.getGrade()));
		}
		return grades;
	}


	/**
	 * Store the session id value associated with the logged 
	 * in Blackboard web service.
	 * 
	 */
	private static class PasswordCallbackClass implements CallbackHandler {

		String sessionId = null;


		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}


		public void handle(Callback[] callbacks) throws IOException,
		UnsupportedCallbackException {
			for (int i = 0; i < callbacks.length; i++) {
				WSPasswordCallback pwcb = (WSPasswordCallback) callbacks[i];
				String pw = "nosession";

				if (sessionId != null) {
					pw = sessionId;
				}
				pwcb.setPassword(pw);
			}
		}
	}
}


