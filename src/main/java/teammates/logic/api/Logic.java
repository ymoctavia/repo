package teammates.logic.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.mail.internet.MimeMessage;

import teammates.common.Common;
import teammates.common.datatransfer.CoordData;
import teammates.common.datatransfer.CourseData;
import teammates.common.datatransfer.EvalResultData;
import teammates.common.datatransfer.EvaluationData;
import teammates.common.datatransfer.StudentData;
import teammates.common.datatransfer.SubmissionData;
import teammates.common.datatransfer.TeamData;
import teammates.common.datatransfer.UserData;
import teammates.common.datatransfer.EvaluationData.EvalStatus;
import teammates.common.datatransfer.StudentData.UpdateStatus;
import teammates.common.exception.EnrollException;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.exception.JoinCourseException;
import teammates.common.exception.NotImplementedException;
import teammates.common.exception.UnauthorizedAccessException;
import teammates.logic.Emails;
import teammates.logic.TeamEvalResult;
import teammates.storage.api.AccountsStorage;
import teammates.storage.api.CoursesStorage;
import teammates.storage.api.EvaluationsStorage;
import teammates.storage.entity.Coordinator; //TODO: remove dependency to entity package
import teammates.storage.entity.Course;
import teammates.storage.entity.Evaluation;
import teammates.storage.entity.Student;
import teammates.storage.entity.Submission; 

import com.google.appengine.api.datastore.Text; //TODO: remove this dependency
import com.google.appengine.api.users.User;

/**
 * This class represents the API to the business logic of the system. 
 */
public class Logic {

	private static Logger log = Common.getLogger();

	@SuppressWarnings("unused")
	private void ____USER_level_methods__________________________________() {
	}

	/**
	 * Produces the URL the user should use to login to the system
	 * 
	 * @param redirectUrl
	 *            This is the URL the user will be directed to after login.
	 */
	public static String getLoginUrl(String redirectUrl) {
		AccountsStorage accounts = AccountsStorage.inst();
		return accounts.getLoginPage(redirectUrl);
	}

	/**
	 * Produces the URL used to logout the user
	 * 
	 * @param redirectUrl
	 *            This is the URL the user will be directed to after logout.
	 */
	public static String getLogoutUrl(String redirectUrl) {
		AccountsStorage accounts = AccountsStorage.inst();
		return accounts.getLogoutPage(redirectUrl);
	}

	/**
	 * Verifies if the user is logged into his/her Google account
	 */
	public static boolean isUserLoggedIn() {
		AccountsStorage accounts = AccountsStorage.inst();
		return (accounts.getUser() != null);
	}

	public UserData getLoggedInUser() {
		AccountsStorage accounts = AccountsStorage.inst();
		User user = accounts.getUser();
		if (user == null) {
			return null;
		}

		UserData userData = new UserData(user.getNickname());

		// TODO: make more efficient?
		if (accounts.isAdministrator()) {
			userData.isAdmin = true;
		}
		if (accounts.isCoordinator()) {
			userData.isCoord = true;
		}

		if (accounts.isStudent(user.getNickname())) {
			userData.isStudent = true;
		}
		return userData;
	}
	
	@SuppressWarnings("unused")
	private void ____ACCESS_control_methods________________________________() {
	}

	//@formatter:off
	
	protected boolean isInternalCall() {
		String callerClassName = Thread.currentThread().getStackTrace()[4]
				.getClassName();
		String thisClassName = this.getClass().getCanonicalName();
		return callerClassName.equals(thisClassName);
	}

	private void verifyCoordUsingOwnIdOrAbove(String coordId) {
		if (isInternalCall()) return;
		if (isAdminLoggedIn()) return;
		if (isOwnId(coordId)) return;
		throw new UnauthorizedAccessException();
	}
	
	private void verifyOwnerOfId(String googleId) {
		if (isInternalCall()) return;
		if (isAdminLoggedIn()) return;
		if (isOwnId(googleId)) return;
		throw new UnauthorizedAccessException();
	}
	
	private void verifyRegisteredUserOrAbove() {
		if (isInternalCall()) return;
		if (isAdminLoggedIn()) return;
		if (isCoordLoggedIn()) return;
		if (isStudentLoggedIn()) return;
		throw new UnauthorizedAccessException();
	}
	
	private void verifyCourseOwnerOrAbove(String courseId) {
		if (isInternalCall()) return;
		if (isAdminLoggedIn()) return;
		if (isCourseOwner(courseId)) return;
		throw new UnauthorizedAccessException();
	}
	
	private void verifyCourseOwnerOrStudentInCourse(String courseId) {
		if (isInternalCall()) return;
		if (isAdminLoggedIn()) return;
		if (isCourseOwner(courseId)) return;
		if (isInCourse(courseId)) return;
		throw new UnauthorizedAccessException();
	}
	
	private void verifyAdminLoggedIn() {
		if (isInternalCall()) return;
		if (isAdminLoggedIn())  return;
		throw new UnauthorizedAccessException();
	}
	
	private void verifyLoggedInUserAndAbove() {
		if (isInternalCall()) return;
		if (isUserLoggedIn()) return;
		throw new UnauthorizedAccessException();
	}
	
	private void verifySameStudentOrAdmin(String googleId) {
		if (isInternalCall()) return;
		if (isAdminLoggedIn()) return;
		if (isOwnId(googleId)) return;
		throw new UnauthorizedAccessException();
	}
	
	private void verifySameStudentOrCourseOwnerOrAdmin(String courseId, String googleId) {
		if (isInternalCall()) return;
		if (isAdminLoggedIn()) return;
		if (isOwnId(googleId)) return;
		if (isCourseOwner(courseId)) return;
		throw new UnauthorizedAccessException();
	}
	
	private void verifyReviewerOrCourseOwnerOrAdmin(String courseId,
			String reviewerEmail) {
		if (isInternalCall()) return;
		if (isAdminLoggedIn()) return;
		if (isCourseOwner(courseId)) return;
		if (isOwnEmail(courseId, reviewerEmail)) return;
		throw new UnauthorizedAccessException();
	}
	
	private void verifySubmissionEditableForUser(SubmissionData submission) {
		if (isInternalCall()) return;
		if (isAdminLoggedIn()) return;
		if (isCourseOwner(submission.course)) return;
		if (isOwnEmail(submission.course, submission.reviewer)
				&& getEvaluationStatus(submission.course, submission.evaluation)==EvalStatus.OPEN) return;
		throw new UnauthorizedAccessException();
	}
	
	private void verfyCourseOwner_OR_EmailOwnerAndPublished(String courseId, 
			String evaluationName, String studentEmail) {
		if (isInternalCall()) return;
		if (isAdminLoggedIn()) return;
		if (isCourseOwner(courseId)) return;
		if (isOwnEmail(courseId,studentEmail)
				&&isEvaluationPublished(courseId,evaluationName)) return;
		throw new UnauthorizedAccessException();
	}
	
	//@formatter:on

	private boolean isOwnEmail(String courseId, String studentEmail) {
		UserData user = getLoggedInUser();
		if (user == null) {
			return false;
		}
		CourseData course = getCourse(courseId);
		if (course == null) {
			return false;
		}
		StudentData student = getStudent(courseId, studentEmail);
		return student == null ? false : user.id.equals(student.id);
	}

	private boolean isOwnId(String userId) {
		UserData loggedInUser = getLoggedInUser();
		return loggedInUser == null ? false : loggedInUser.id
				.equalsIgnoreCase(userId);
	}

	private boolean isCourseOwner(String courseId) {
		CourseData course = getCourse(courseId);
		UserData user = getLoggedInUser();
		return user != null && course != null
				&& course.coord.equalsIgnoreCase(user.id);
	}

	private boolean isInCourse(String courseId) {
		UserData user = getLoggedInUser();
		if (user == null)
			return false;

		CourseData course = getCourse(courseId);
		if (course == null)
			return false;

		return (null != getStudentInCourseForGoogleId(courseId, user.id));
	}

	private boolean isEvaluationPublished(String courseId, String evaluationName) {
		EvaluationData evaluation = getEvaluation(courseId, evaluationName);
		if (evaluation == null) {
			return false;
		} else {
			return evaluation.getStatus() == EvalStatus.PUBLISHED;
		}
	}

	/**
	 * Verifies if the logged in user has Admin privileges
	 */
	private boolean isAdminLoggedIn() {
		UserData loggedInUser = getLoggedInUser();
		return loggedInUser == null ? false : loggedInUser.isAdmin;
	}

	/**
	 * Verifies if the logged in user has Coord privileges
	 */
	private boolean isCoordLoggedIn() {
		UserData loggedInUser = getLoggedInUser();
		return loggedInUser == null ? false : loggedInUser.isCoord;
	}

	/**
	 * Verifies if the logged in user has Student privileges
	 */
	private boolean isStudentLoggedIn() {
		UserData loggedInUser = getLoggedInUser();
		return loggedInUser == null ? false : loggedInUser.isStudent;
	}

	
	@SuppressWarnings("unused")
	private void ____COORD_level_methods____________________________________() {
	}

	/**
	 * Access: admin only
	 */
	public void createCoord(String coordID, String coordName, String coordEmail)
			throws EntityAlreadyExistsException, InvalidParametersException {

		Common.verifyNotNull(coordID, "coordinator ID");
		Common.verifyNotNull(coordName, "coordinator Name");
		Common.verifyNotNull(coordEmail, "coordinator Email");

		verifyAdminLoggedIn();

		Common.validateEmail(coordEmail);
		Common.validateCoordName(coordName);
		Common.validateGoogleId(coordID);
		
		AccountsStorage.inst().addCoordinator(coordID, coordName, coordEmail);
	}

	/**
	 * Access: any logged in user
	 */
	public CoordData getCoord(String coordID) {
		
		Common.verifyNotNull(coordID, "coordinator ID");

		verifyLoggedInUserAndAbove();

		Coordinator coord = AccountsStorage.inst().getCoordinator(coordID);

		return (coord == null ? null : new CoordData(coord.getGoogleID(),
				coord.getName(), coord.getEmail()));
	}

	/**
	 * Not implemented
	 */
	public void editCoord(CoordData coord) throws NotImplementedException {
		throw new NotImplementedException("Not implemented because we do "
				+ "not allow editing coordinators");
	}

	/**
	 * Access: Admin only
	 */
	public void deleteCoord(String coordId) {
		
		Common.verifyNotNull(coordId, "coordinator ID");

		verifyAdminLoggedIn();

		List<Course> coordCourseList = CoursesStorage.inst()
				.getCoordinatorCourseList(coordId);
		for (Course course : coordCourseList) {
			deleteCourse(course.getID());
		}
		AccountsStorage.inst().deleteCoord(coordId);
	}

	/**
	 * Access level: Admin, Coord (for self)
	 * @return Returns a less-detailed version of Coord's course data
	 */
	public HashMap<String, CourseData> getCourseListForCoord(String coordId)
			throws EntityDoesNotExistException {

		Common.verifyNotNull(coordId, "coordinator Id");

		verifyCoordUsingOwnIdOrAbove(coordId);

		HashMap<String, CourseData> courseSummaryListForCoord = CoursesStorage
				.inst().getCourseSummaryListForCoord(coordId);
		if (courseSummaryListForCoord.size() == 0) {
			if (getCoord(coordId) == null) {
				throw new EntityDoesNotExistException(
						"Coordinator does not exist :" + coordId);
			}
		}
		return courseSummaryListForCoord;
	}

	/**
	 * Access level: Admin, Coord (for self)
	 * 
	 * @return Returns a more-detailed version of Coord's course data <br>
	 */
	public HashMap<String, CourseData> getCourseDetailsListForCoord(
			String coordId) throws EntityDoesNotExistException {

		Common.verifyNotNull(coordId, "coordinator ID");

		verifyCoordUsingOwnIdOrAbove(coordId);

		// TODO: using this method here may not be efficient as it retrieves
		// info not required
		HashMap<String, CourseData> courseList = getCourseListForCoord(coordId);
		ArrayList<EvaluationData> evaluationList = getEvaluationsListForCoord(coordId);
		for (EvaluationData ed : evaluationList) {
			CourseData courseSummary = courseList.get(ed.course);
			courseSummary.evaluations.add(ed);
		}
		return courseList;
	}

	/**
	 * Access level: Admin, Coord (for self)
	 * 
	 * @return Returns a less-detailed version of Coord's evaluations <br>
	 */
	public ArrayList<EvaluationData> getEvaluationsListForCoord(String coordId)
			throws EntityDoesNotExistException {

		Common.verifyNotNull(coordId, "coordinator ID");

		verifyCoordUsingOwnIdOrAbove(coordId);

		List<Course> courseList = CoursesStorage.inst()
				.getCoordinatorCourseList(coordId);

		if ((courseList.size() == 0) && (getCoord(coordId) == null)) {
			throw new EntityDoesNotExistException(
					"Coordinator does not exist :" + coordId);
		}

		ArrayList<EvaluationData> evaluationDetailsList = new ArrayList<EvaluationData>();

		for (Course c : courseList) {
			ArrayList<EvaluationData> evaluationsSummaryForCourse = EvaluationsStorage
					.inst().getEvaluationsSummaryForCourse(c.getID());

			evaluationDetailsList.addAll(evaluationsSummaryForCourse);
		}

		return evaluationDetailsList;
	}

	@SuppressWarnings("unused")
	private void ____COURSE_level_methods__________________________________() {
	}

	/**
	 * Access level: Coord and above
	 */
	public void createCourse(String coordId, String courseId, String courseName)
			throws EntityAlreadyExistsException, InvalidParametersException {

		Common.verifyNotNull(coordId, "coordinator ID");
		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(courseName, "course name");
		
		verifyCoordUsingOwnIdOrAbove(coordId);

		//TODO: this validation should be done at a lower level
		Common.validateGoogleId(coordId);
		Common.validateCourseId(courseId);
		Common.validateCourseName(courseName);
		
		CoursesStorage.inst().addCourse(courseId, courseName, coordId);
	}

	/**
	 * AccessLevel : any registered user (because it is too expensive to check
	 * if a student is in the course)
	 */
	public CourseData getCourse(String courseId) {

		Common.verifyNotNull(courseId, "course ID");

		verifyRegisteredUserOrAbove();

		Course c = CoursesStorage.inst().getCourse(courseId);
		return (c == null ? null : new CourseData(c.getID(), c.getName(),
				c.getCoordinatorID()));
	}

	/**
	 * Access: course owner, student in course, admin
	 */
	public CourseData getCourseDetails(String courseId)
			throws EntityDoesNotExistException {
		
		Common.verifyNotNull(courseId, "course ID");
		
		verifyCourseOwnerOrStudentInCourse(courseId);

		// TODO: very inefficient. Should be optimized.
		CourseData course = getCourse(courseId);

		if (course == null) {
			throw new EntityDoesNotExistException("The course does not exist: "
					+ courseId);
		}
		HashMap<String, CourseData> courseList = getCourseDetailsListForCoord(course.coord);
		return courseList.get(courseId);
	}

	public void editCourse(CourseData course) throws NotImplementedException {
		throw new NotImplementedException("Not implemented because we do "
				+ "not allow editing courses");
	}

	/**
	 * Access: course owner and above
	 */
	public void deleteCourse(String courseId) {

		Common.verifyNotNull(courseId, "course ID");

		verifyCourseOwnerOrAbove(courseId);

		EvaluationsStorage.inst().deleteEvaluations(courseId);
		CoursesStorage.inst().deleteCourse(courseId);
	}

	/**
	 * Access: course owner and above
	 */
	public List<StudentData> getStudentListForCourse(String courseId)
			throws EntityDoesNotExistException {

		Common.verifyNotNull(courseId, "course ID");

		verifyCourseOwnerOrAbove(courseId);

		List<Student> studentList = CoursesStorage.inst().getStudentList(courseId);

		if ((studentList.size() == 0) && (getCourse(courseId) == null)) {
			throw new EntityDoesNotExistException("Course does not exist :"
					+ courseId);
		}

		List<StudentData> returnList = new ArrayList<StudentData>();
		for (Student s : studentList) {
			returnList.add(new StudentData(s));
		}
		return returnList;
	}

	/**
	 * Access: course owner and above
	 * @return 
	 */
	public List<MimeMessage> sendRegistrationInviteForCourse(String courseId)
			throws InvalidParametersException {

		Common.verifyNotNull(courseId, "course ID");

		verifyCourseOwnerOrAbove(courseId);

		List<Student> studentList = CoursesStorage.inst().getUnregisteredStudentList(
				courseId);
		
		ArrayList<MimeMessage> emailsSent = new ArrayList<MimeMessage>();

		for (Student s : studentList) {
			try {
				MimeMessage email = sendRegistrationInviteToStudent(courseId, s.getEmail());
				emailsSent.add(email);
			} catch (EntityDoesNotExistException e) {
				log.severe("Unexpected exception" + Common.stackTraceToString(e));
			}
		}
		return emailsSent;
	}

	/**
	 * Access: course owner and above
	 */
	public List<StudentData> enrollStudents(String enrollLines, String courseId)
			throws EnrollException, EntityDoesNotExistException {

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(enrollLines, "enrollment text");

		verifyCourseOwnerOrAbove(courseId);

		if (getCourse(courseId) == null) {
			throw new EntityDoesNotExistException("Course does not exist :"
					+ courseId);
		}
		ArrayList<StudentData> returnList = new ArrayList<StudentData>();
		String[] linesArray = enrollLines.split(Common.EOL);
		ArrayList<StudentData> studentList = new ArrayList<StudentData>();

		// check if all non-empty lines are formatted correctly
		for (int i = 0; i < linesArray.length; i++) {
			String line = linesArray[i];
			try {
				if (Common.isWhiteSpace(line))
					continue;
				studentList.add(new StudentData(line, courseId));
			} catch (InvalidParametersException e) {
				throw new EnrollException(e.errorCode, "Problem in line : "
						+ line + Common.EOL + e.getMessage());
			}
		}

		// enroll all students
		for (StudentData student : studentList) {
			StudentData studentInfo;
			studentInfo = enrollStudent(student);
			returnList.add(studentInfo);
		}

		// add to return list students not included in the enroll list.
		List<StudentData> studentsInCourse = getStudentListForCourse(courseId);
		for (StudentData student : studentsInCourse) {
			if (!isInEnrollList(student, returnList)) {
				student.updateStatus = StudentData.UpdateStatus.NOT_IN_ENROLL_LIST;
				returnList.add(student);
			}
		}

		return returnList;
	}

	/**
	 * Access: course owner, student in course, admin
	 * 
	 * @param courseId
	 * @return The CourseData object that is returned will contain attributes
	 *         teams(type:TeamData) and loners(type:StudentData)
	 * @throws EntityDoesNotExistException
	 *             if the course does not exist <br>
	 *             Access : course owner and above
	 */
	public CourseData getTeamsForCourse(String courseId)
			throws EntityDoesNotExistException {

		Common.verifyNotNull(courseId, "coure ID");

		verifyCourseOwnerOrStudentInCourse(courseId);

		List<StudentData> students = getStudentListForCourse(courseId);
		CoursesStorage.sortByTeamName(students);

		CourseData course = getCourse(courseId);

		if (course == null) {
			throw new EntityDoesNotExistException("The course " + courseId
					+ " does not exist");
		}

		TeamData team = null;
		for (int i = 0; i < students.size(); i++) {

			StudentData s = students.get(i);

			// if loner
			if (s.team.equals("")) {
				course.loners.add(s);
				// first student of first team
			} else if (team == null) {
				team = new TeamData();
				team.name = s.team;
				team.students.add(s);
				// student in the same team as the previous student
			} else if (s.team.equals(team.name)) {
				team.students.add(s);
				// first student of subsequent teams (not the first team)
			} else {
				course.teams.add(team);
				team = new TeamData();
				team.name = s.team;
				team.students.add(s);
			}

			// if last iteration
			if (i == (students.size() - 1)) {
				course.teams.add(team);
			}
		}

		return course;
	}

	@SuppressWarnings("unused")
	private void ____STUDENT_level_methods__________________________________() {
	}

	/**
	 * Access: course owner and above
	 */
	public void createStudent(StudentData studentData)
			throws EntityAlreadyExistsException, InvalidParametersException {

		Common.verifyNotNull(studentData, "student data");

		verifyCourseOwnerOrAbove(studentData.course);

		Student student = new Student(studentData);
		// TODO: this if for backward compatibility with old system. Old system
		// considers "" as unregistered. It should be changed to consider
		// null as unregistered.
		if (student.getID() == null) {
			student.setID("");
		}
		if (student.getComments() == null) {
			student.setComments("");
		}
		if (student.getTeamName() == null) {
			student.setTeamName("");
		}
		CoursesStorage.inst().createStudent(student);
	}

	/**
	 * Access: any registered user (to minimize cost of checking)
	 * 
	 * @return returns null if there is no such student.
	 */
	public StudentData getStudent(String courseId, String email) {

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(email, "student email");

		verifyRegisteredUserOrAbove();

		Student student = AccountsStorage.inst().getStudent(courseId, email);
		return (student == null ? null : new StudentData(student));
	}

	/**
	 * All attributes except courseId be changed. Trying to change courseId will
	 * be treated as trying to edit a student in a different course.<br>
	 * Changing team name will not delete existing team profile even if there
	 * are no more members in the team. This can cause orphan team profiles but
	 * the effect is considered insignificant and not worth the effort required
	 * to avoid it. A side benefit of this strategy is the team can reclaim the
	 * profile by changing the team name back to the original one. But note that
	 * orphaned team profiles can be inherited by others if another team adopts
	 * the team name previously discarded by a team.
	 * 
	 * @param originalEmail
	 * @param student
	 * @throws InvalidParametersException
	 * @throws EntityDoesNotExistException
	 * 
	 * <br>
	 *             Access: coord of course and above.
	 */
	public void editStudent(String originalEmail, StudentData student)
			throws InvalidParametersException, EntityDoesNotExistException {

		verifyCourseOwnerOrAbove(student.course);

		// TODO: make the implementation more defensive
		CoursesStorage.inst().editStudent(student.course, originalEmail, student.name,
				student.team, student.email, student.id, student.comments,
				student.profile);
	}

	/**
	 * Access: course owner and above
	 * 
	 * @param courseId
	 * @param studentEmail
	 */
	public void deleteStudent(String courseId, String studentEmail) {

		verifyCourseOwnerOrAbove(courseId);

		CoursesStorage.inst().deleteStudent(courseId, studentEmail);
		EvaluationsStorage.inst().deleteSubmissionsForStudent(courseId, studentEmail);
	}

	/**
	 * Access: course owner and above
	 * 
	 * @param courseId
	 * @param studentEmail
	 * @return 
	 * @throws EntityDoesNotExistException
	 * @throws InvalidParametersException
	 */
	public MimeMessage sendRegistrationInviteToStudent(String courseId,
			String studentEmail) throws EntityDoesNotExistException,
			InvalidParametersException {

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(studentEmail, "student email");

		verifyCourseOwnerOrAbove(courseId);

		Course course = CoursesStorage.inst().getCourse(courseId);
		Student student = CoursesStorage.inst().getStudentWithEmail(courseId,
				studentEmail);
		if (student == null) {
			throw new EntityDoesNotExistException("Student [" + studentEmail
					+ "] does not exist in course [" + courseId + "]");
		}
		
		Emails emailMgr = new Emails();
		try {
			MimeMessage email = emailMgr.generateStudentCourseJoinEmail(new CourseData(course), new StudentData(student));
			emailMgr.sendEmail(email);
			return email;
		} catch (Exception e) {
			throw new RuntimeException("Unexpected error while sending email",e);
		} 

	}

	/**
	 * Access: same student and admin only
	 * 
	 * @param googleId
	 * @return returns null if
	 */
	public ArrayList<StudentData> getStudentsWithId(String googleId) {

		verifySameStudentOrAdmin(googleId);

		List<Student> students = AccountsStorage.inst().getStudentsWithID(googleId);
		ArrayList<StudentData> returnList = new ArrayList<StudentData>();
		for (Student s : students) {
			returnList.add(new StudentData(s));
		}
		return returnList;
	}

	/**
	 * Access: same student and admin only
	 * 
	 * @param courseId
	 * @param googleId
	 * @return
	 */
	public StudentData getStudentInCourseForGoogleId(String courseId,
			String googleId) {
		// TODO: make more efficient?

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(googleId, "Google ID");

		verifySameStudentOrCourseOwnerOrAdmin(courseId, googleId);

		ArrayList<StudentData> studentList = getStudentsWithId(googleId);
		for (StudentData sd : studentList) {
			if (sd.course.equals(courseId)) {
				return sd;
			}
		}
		return null;
	}

	/**
	 * Access: owner of googleId
	 * 
	 * @param googleId
	 * @param key
	 * @throws JoinCourseException
	 * @throws InvalidParametersException
	 */
	public void joinCourse(String googleId, String key)
			throws JoinCourseException, InvalidParametersException {

		Common.verifyNotNull(googleId, "google ID");
		Common.verifyNotNull(key, "registration key");

		verifyOwnerOfId(googleId);

		CoursesStorage.inst().joinCourse(key.trim(), googleId.trim());

	}

	/**
	 * Access: course owner and above
	 * 
	 * @param courseId
	 * @param email
	 * @return
	 */
	public String getKeyForStudent(String courseId, String email) {

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(email, "student email");

		verifyCourseOwnerOrAbove(courseId);

		Student student = AccountsStorage.inst().getStudent(courseId, email);

		if (student == null) {
			return null;
		}

		long keyLong = Long.parseLong(student.getRegistrationKey().toString());
		return Student.getStringKeyForLongKey(keyLong);
	}

	/**
	 * Access: student who owns the googleId, admin
	 * 
	 * @param googleId
	 * @return
	 * @throws EntityDoesNotExistException
	 * @throws InvalidParametersException
	 */
	public List<CourseData> getCourseListForStudent(String googleId)
			throws EntityDoesNotExistException, InvalidParametersException {

		Common.verifyNotNull(googleId, "Google Id");

		verifySameStudentOrAdmin(googleId);

		if (getStudentsWithId(googleId).size() == 0) {
			throw new EntityDoesNotExistException("Student with Google ID "
					+ googleId + " does not exist");
		}

		return CoursesStorage.inst().getCourseListForStudent(googleId);
	}

	/**
	 * Access: any logged in user (to minimize cost of checking)
	 * 
	 * @param courseId
	 * @param evaluationName
	 * @param studentEmail
	 * @return
	 * @throws InvalidParametersException
	 */
	public boolean hasStudentSubmittedEvaluation(String courseId,
			String evaluationName, String studentEmail)
			throws InvalidParametersException {

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(evaluationName, "evaluation name");
		Common.verifyNotNull(studentEmail, "student email");

		verifyLoggedInUserAndAbove();

		List<SubmissionData> submissions = null;
		try {
			submissions = getSubmissionsFromStudent(courseId, evaluationName,
					studentEmail);
		} catch (EntityDoesNotExistException e) {
			return false;
		}

		if (submissions == null) {
			return false;
		}

		for (SubmissionData sd : submissions) {
			if (sd.points != Common.POINTS_NOT_SUBMITTED) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Access: student who owns the googleId, admin
	 * 
	 * @param googleId
	 * @return
	 * @throws EntityDoesNotExistException
	 * @throws InvalidParametersException
	 */
	public List<CourseData> getCourseDetailsListForStudent(String googleId)
			throws EntityDoesNotExistException, InvalidParametersException {

		verifySameStudentOrAdmin(googleId);

		List<CourseData> courseList = getCourseListForStudent(googleId);
		for (CourseData c : courseList) {
			List<Evaluation> evaluationList = EvaluationsStorage.inst()
					.getEvaluationList(c.id);
			for (Evaluation e : evaluationList) {
				EvaluationData ed = new EvaluationData(e);
				log.fine("Adding evaluation " + ed.name + " to course " + c.id);
				if (ed.getStatus() != EvalStatus.AWAITING) {
					c.evaluations.add(ed);
				}
			}
		}
		return courseList;
	}

	/**
	 * Access: owner of the course, owner of result (when PUBLISHED), admin
	 * 
	 * @param courseId
	 * @param evaluationName
	 * @param studentEmail
	 * @return
	 * @throws EntityDoesNotExistException
	 * @throws InvalidParametersException
	 */
	public EvalResultData getEvaluationResultForStudent(String courseId,
			String evaluationName, String studentEmail)
			throws EntityDoesNotExistException, InvalidParametersException {

		Common.verifyNotNull(courseId, "course id");
		Common.verifyNotNull(evaluationName, "evaluation name");
		Common.verifyNotNull(studentEmail, "student email");

		verfyCourseOwner_OR_EmailOwnerAndPublished(courseId, evaluationName,
				studentEmail);

		StudentData student = getStudent(courseId, studentEmail);
		if (student == null) {
			throw new EntityDoesNotExistException("The student " + studentEmail
					+ " does not exist in course " + courseId);
		}
		// TODO: this is very inefficient as it calculates the results for the
		// whole class first
		EvaluationData courseResult = getEvaluationResult(courseId,
				evaluationName);
		TeamData teamData = courseResult.getTeamData(student.team);
		EvalResultData returnValue = null;

		for (StudentData sd : teamData.students) {
			if (sd.email.equals(student.email)) {
				returnValue = sd.result;
				break;
			}
		}

		for (StudentData sd : teamData.students) {
			returnValue.selfEvaluations.add(sd.result.getSelfEvaluation());
		}

		returnValue.sortIncomingByFeedbackAscending();
		return returnValue;
	}

	@SuppressWarnings("unused")
	private void ____EVALUATION_level_methods______________________________() {
	}

	/**
	 * Access: course owner and above
	 * 
	 * @throws EntityAlreadyExistsException
	 * @throws InvalidParametersException
	 *             is thrown if any of the parameters puts the evaluation in an
	 *             invalid state (e.g., endTime is set before startTime).
	 *             However, setting start time to a past time is allowed.
	 */
	public void createEvaluation(EvaluationData evaluation)
			throws EntityAlreadyExistsException, InvalidParametersException {

		Common.verifyNotNull(evaluation, "evaluation");

		verifyCourseOwnerOrAbove(evaluation.course);

		evaluation.validate();
		EvaluationsStorage.inst().addEvaluation(evaluation.toEvaluation());
	}
	
	protected void createSubmissions(List<SubmissionData> submissionDataList) {
		ArrayList<Submission> submissions = new ArrayList<Submission>();
		for (SubmissionData sd : submissionDataList) {
			submissions.add(sd.toSubmission());
		}
		EvaluationsStorage.inst().editSubmissions(submissions);
	}

	/**
	 * Access: all registered users
	 * 
	 * @param courseId
	 * @param evaluationName
	 * @return
	 */
	public EvaluationData getEvaluation(String courseId, String evaluationName) {

		verifyRegisteredUserOrAbove();

		Evaluation e = EvaluationsStorage.inst().getEvaluation(courseId,
				evaluationName);
		return (e == null ? null : new EvaluationData(e));
	}

	/**
	 * Can be used to change all fields exception "activated" field
	 * Access: owner and above
	 * 
	 * @param evaluation
	 * @throws EntityDoesNotExistException
	 * @throws InvalidParametersException
	 */
	public void editEvaluation(String courseId, String evaluationName, String instructions, Date start, Date end, double timeZone, int gracePeriod, boolean p2pEndabled)
			throws EntityDoesNotExistException, InvalidParametersException {

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(evaluationName, "evaluation name");
		Common.verifyNotNull(start, "starting time");
		Common.verifyNotNull(end, "deadline");

		verifyCourseOwnerOrAbove(courseId);
		EvaluationData original = getEvaluation(courseId, evaluationName);
		
		if(original==null){
			throw new EntityDoesNotExistException("Evaluation "+evaluationName+" does not exist in course "+courseId);
		}

		EvaluationData evaluation = new EvaluationData();
		evaluation.course = courseId; 
		evaluation.name = evaluationName;
		evaluation.instructions = instructions;
		evaluation.p2pEnabled = p2pEndabled;
		evaluation.startTime = start;
		evaluation.endTime = end;
		evaluation.gracePeriod = gracePeriod;
		evaluation.timeZone = timeZone;
		
		//this field cannot be changed via this method
		evaluation.activated = original.activated;
		evaluation.published = original.published;
		
		evaluation.validate();
		
		editEvaluationAllFields(evaluation);
	}
	
	protected void editEvaluationAllFields(EvaluationData evaluation)
			throws EntityDoesNotExistException, InvalidParametersException {

		EvaluationsStorage.inst().editEvaluation(evaluation.course, evaluation.name,
				evaluation.instructions, evaluation.p2pEnabled,
				evaluation.startTime, evaluation.endTime,
				evaluation.gracePeriod, evaluation.activated,
				evaluation.published, evaluation.timeZone);
	}
	
	

	/**
	 * Access: owner and above
	 * 
	 * @param courseId
	 * @param evaluationName
	 */
	public void deleteEvaluation(String courseId, String evaluationName) {

		verifyCourseOwnerOrAbove(courseId);

		EvaluationsStorage.inst().deleteEvaluation(courseId, evaluationName);
	}

	/**
	 * Access: course owner and above
	 * 
	 * @param courseId
	 * @param evaluationName
	 * @throws EntityDoesNotExistException
	 * @throws InvalidParametersException
	 *             if the evaluation is not ready to be published.
	 */
	public void publishEvaluation(String courseId, String evaluationName)
			throws EntityDoesNotExistException, InvalidParametersException {

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(evaluationName, "evaluation name");

		verifyCourseOwnerOrAbove(courseId);

		EvaluationData evaluation = getEvaluation(courseId, evaluationName);

		verifyEvaluationExists(evaluation, courseId, evaluationName);

		if (evaluation.getStatus() != EvalStatus.CLOSED) {
			throw new InvalidParametersException(
					Common.ERRORCODE_PUBLISHED_BEFORE_CLOSING,
					"Cannot publish an evaluation unless it is CLOSED");
		}

		EvaluationsStorage.inst().publishEvaluation(courseId, evaluationName);
		sendEvaluationPublishedEmails(courseId, evaluationName);
	}

	/**
	 * Access: course owner and above
	 * 
	 * @param courseId
	 * @param evaluationName
	 * @throws EntityDoesNotExistException
	 * @throws InvalidParametersException
	 *             if the evaluation is not ready to be unpublished.
	 */
	public void unpublishEvaluation(String courseId, String evaluationName)
			throws EntityDoesNotExistException, InvalidParametersException {

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(evaluationName, "evaluation name");

		verifyCourseOwnerOrAbove(courseId);

		EvaluationData evaluation = getEvaluation(courseId, evaluationName);

		verifyEvaluationExists(evaluation, courseId, evaluationName);

		if (evaluation.getStatus() != EvalStatus.PUBLISHED) {
			throw new InvalidParametersException(
					Common.ERRORCODE_UNPUBLISHED_BEFORE_PUBLISHING,
					"Cannot unpublish an evaluation unless it is PUBLISHED");
		}

		EvaluationsStorage.inst().unpublishEvaluation(courseId, evaluationName);
	}

	/**
	 * Access: course owner and above
	 * 
	 * @param courseId
	 * @param evaluationName
	 */
	public List<MimeMessage> sendReminderForEvaluation(String courseId,
			String evaluationName) throws EntityDoesNotExistException {

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(evaluationName, "evaluation name");

		verifyCourseOwnerOrAbove(courseId);

		EvaluationData evaluation = getEvaluation(courseId, evaluationName);

		verifyEvaluationExists(evaluation, courseId, evaluationName);

		// Filter out students who have submitted the evaluation
		List<Student> studentList = CoursesStorage.inst().getStudentList(courseId);
		List<StudentData> studentsToRemindList = new ArrayList<StudentData>();
		for (Student s : studentList) {
			if (!EvaluationsStorage.inst().isEvaluationSubmitted(evaluation,
					s.getEmail())) {
				studentsToRemindList.add(new StudentData(s));
			}
		}

		CourseData course = getCourse(courseId);
		
		List<MimeMessage> emails;

		Emails emailMgr = new Emails();
		try {
			emails = emailMgr
					.generateEvaluationReminderEmails(course, evaluation,
							studentsToRemindList);
			emailMgr.sendEmails(emails);
		} catch (Exception e) {
			throw new RuntimeException("Error while sending emails :", e);
		}
		
		return emails;
	}

	/**
	 * Access: course owner and above
	 * 
	 * @param courseId
	 * @param evaluationName
	 * @return Returns null if any of the parameters is null.
	 * @throws EntityDoesNotExistException
	 *             if the course or the evaluation does not exists.
	 */
	public EvaluationData getEvaluationResult(String courseId,
			String evaluationName) throws EntityDoesNotExistException {

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(evaluationName, "evaluation name");

		verifyCourseOwnerOrAbove(courseId);

		CourseData course = getTeamsForCourse(courseId);
		EvaluationData returnValue = getEvaluation(courseId, evaluationName);
		HashMap<String, SubmissionData> submissionDataList = getSubmissionsForEvaluation(
				courseId, evaluationName);
		returnValue.teams = course.teams;
		for (TeamData team : returnValue.teams) {
			for (StudentData student : team.students) {
				student.result = new EvalResultData();
				// TODO: refactor this method. May be have a return value?
				populateSubmissionsAndNames(submissionDataList, team, student);
			}

			TeamEvalResult teamResult = calculateTeamResult(team);
			team.result = teamResult;
			populateTeamResult(team, teamResult);

		}
		return returnValue;
	}

	/**
	 * Access: course owner, reviewer, admin
	 * 
	 * @param courseId
	 * @param evaluationName
	 * @param reviewerEmail
	 * @return
	 * @throws EntityDoesNotExistException
	 * @throws InvalidParametersException
	 */
	public List<SubmissionData> getSubmissionsFromStudent(String courseId,
			String evaluationName, String reviewerEmail)
			throws EntityDoesNotExistException, InvalidParametersException {

		Common.verifyNotNull(courseId, "course ID");
		Common.verifyNotNull(evaluationName, "evaluation name");
		Common.verifyNotNull(reviewerEmail, "student email");

		verifyReviewerOrCourseOwnerOrAdmin(courseId, reviewerEmail);

		List<Submission> submissions = EvaluationsStorage.inst()
				.getSubmissionFromStudentList(courseId, evaluationName,
						reviewerEmail);
		if (submissions.size() == 0) {
			CoursesStorage.inst().verifyCourseExists(courseId);
			EvaluationsStorage.inst().verifyEvaluationExists(courseId, evaluationName);
			AccountsStorage.inst().verifyStudentExists(courseId, reviewerEmail);
		}
		StudentData student = getStudent(courseId, reviewerEmail);
		ArrayList<SubmissionData> returnList = new ArrayList<SubmissionData>();
		for (Submission s : submissions) {
			StudentData reviewee = getStudent(courseId, s.getToStudent());
			if (!isOrphanSubmission(student, reviewee, s)) {
				SubmissionData sd = new SubmissionData(s);
				sd.reviewerName = student.name;
				sd.revieweeName = reviewee.name;
				returnList.add(sd);
			}
		}
		return returnList;
	}

	@SuppressWarnings("unused")
	private void ____SUBMISSION_level_methods_____________________________() {
	}

	public void createSubmission(SubmissionData submission)
			throws NotImplementedException {
		throw new NotImplementedException(
				"Not implemented because submissions "
						+ "are created automatically");
	}

	/**
	 * Access: course owner, reviewer (if OPEN), admin
	 * 
	 * @param submissionDataList
	 * @throws EntityDoesNotExistException
	 * @throws InvalidParametersException
	 */
	public void editSubmissions(List<SubmissionData> submissionDataList)
			throws EntityDoesNotExistException, InvalidParametersException {

		Common.verifyNotNull(submissionDataList, "submissions list");

		for (SubmissionData sd : submissionDataList) {
			verifySubmissionEditableForUser(sd);
			editSubmission(sd);
		}
	}

	public void deleteSubmission(SubmissionData submission)
			throws NotImplementedException {
		throw new NotImplementedException(
				"Not implemented because submissions "
						+ "are deleted automatically");
	}


	@SuppressWarnings("unused")
	private void ____helper_methods________________________________________() {
	}

	private List<MimeMessage> sendEvaluationPublishedEmails(String courseId,
			String evaluationName) throws EntityDoesNotExistException {
		List<MimeMessage> emailsSent;
		
		CourseData c = getCourse(courseId);
		EvaluationData e = getEvaluation(courseId, evaluationName);
		List<StudentData> students = getStudentListForCourse(courseId);
		
		Emails emailMgr = new Emails();
		try {
			emailsSent = emailMgr.generateEvaluationPublishedEmails(c, e, students);
			emailMgr.sendEmails(emailsSent);
		} catch (Exception ex) {
			throw new RuntimeException("Unexpected error while sending emails ", ex );
		} 
		return emailsSent;
	}

	private void verifyEvaluationExists(EvaluationData evaluation,
			String courseId, String evaluationName)
			throws EntityDoesNotExistException {
		if (evaluation == null) {
			throw new EntityDoesNotExistException(
					"There is no evaluation named '" + evaluationName
							+ "' under the course " + courseId);
		}
	}

	private void editSubmission(SubmissionData submission)
			throws EntityDoesNotExistException, InvalidParametersException {

		Common.verifyNotNull(submission, "submission");

		verifySubmissionEditableForUser(submission);

		if (getEvaluationStatus(submission.course, submission.evaluation) == EvalStatus.DOES_NOT_EXIST) {
			throw new EntityDoesNotExistException("The evaluation "
					+ submission.evaluation + " does not exist under course "
					+ submission.course);
		}

		// TODO: refactor to remove list
		ArrayList<Submission> submissions = new ArrayList<Submission>();

		submissions.add(submission.toSubmission());
		EvaluationsStorage.inst().editSubmissions(submissions);
	}

	private EvalStatus getEvaluationStatus(String courseId,
			String evaluationName) {
		EvaluationData evaluation = getEvaluation(courseId, evaluationName);
		return evaluation == null ? EvalStatus.DOES_NOT_EXIST : evaluation
				.getStatus();
	}

	private StudentData enrollStudent(StudentData student) {
		StudentData.UpdateStatus updateStatus = UpdateStatus.UNMODIFIED;
		try {
			if (isSameAsExistingStudent(student)) {
				updateStatus = UpdateStatus.UNMODIFIED;
			} else if (isModificationToExistingStudent(student)) {
				editStudent(student.email, student);
				updateStatus = UpdateStatus.MODIFIED;
			} else {
				createStudent(student);
				updateStatus = UpdateStatus.NEW;
			}
		} catch (Exception e) {
			updateStatus = UpdateStatus.ERROR;
			log.severe("EntityExistsExcpetion thrown unexpectedly");
			e.printStackTrace();
		}
		student.updateStatus = updateStatus;
		return student;
	}

	private boolean isOrphanSubmission(StudentData reviewer,
			StudentData reviewee, Submission submission) {
		if (!submission.getTeamName().equals(reviewer.team)) {
			return true;
		}
		if (!submission.getTeamName().equals(reviewee.team)) {
			return true;
		}
		return false;
	}

	private HashMap<String, SubmissionData> getSubmissionsForEvaluation(
			String courseId, String evaluationName)
			throws EntityDoesNotExistException {
		if (getEvaluation(courseId, evaluationName) == null) {
			throw new EntityDoesNotExistException(
					"There is no evaluation named [" + evaluationName
							+ "] under the course [" + courseId + "]");
		}
		// create SubmissionData Hashmap
		List<Submission> submissionsList = EvaluationsStorage.inst()
				.getSubmissionList(courseId, evaluationName);
		HashMap<String, SubmissionData> submissionDataList = new HashMap<String, SubmissionData>();
		for (Submission s : submissionsList) {
			SubmissionData sd = new SubmissionData(s);
			submissionDataList.put(sd.reviewer + "->" + sd.reviewee, sd);
		}
		return submissionDataList;
	}

	private TeamEvalResult calculateTeamResult(TeamData team) {

		Common.verifyNotNull(team, "team object");

		int teamSize = team.students.size();
		int[][] claimedFromStudents = new int[teamSize][teamSize];
		team.sortByStudentNameAscending();
		for (int i = 0; i < teamSize; i++) {
			StudentData studentData = team.students.get(i);
			studentData.result.sortOutgoingByStudentNameAscending();
			for (int j = 0; j < teamSize; j++) {
				SubmissionData submissionData = studentData.result.outgoing
						.get(j);
				claimedFromStudents[i][j] = submissionData.points;
			}

		}
		return new TeamEvalResult(claimedFromStudents);
	}

	private void populateTeamResult(TeamData team, TeamEvalResult teamResult) {
		team.sortByStudentNameAscending();
		int teamSize = team.students.size();
		for (int i = 0; i < teamSize; i++) {
			StudentData s = team.students.get(i);
			s.result.sortIncomingByStudentNameAscending();
			s.result.sortOutgoingByStudentNameAscending();
			s.result.claimedFromStudent = teamResult.claimedToStudents[i][i];
			s.result.claimedToCoord = teamResult.claimedToCoord[i][i];
			s.result.perceivedToStudent = teamResult.perceivedToStudents[i][i];
			s.result.perceivedToCoord = teamResult.perceivedToCoord[i];

			// populate incoming and outgoing
			for (int j = 0; j < teamSize; j++) {
				SubmissionData incomingSub = s.result.incoming.get(j);
				int normalizedIncoming = teamResult.perceivedToStudents[i][j];
				incomingSub.normalized = normalizedIncoming;
				incomingSub.normalizedToCoord = teamResult.unbiased[j][i];
				log.finer("Setting normalized incoming of " + s.name + " from "
						+ incomingSub.reviewerName + " to "
						+ normalizedIncoming);

				SubmissionData outgoingSub = s.result.outgoing.get(j);
				int normalizedOutgoing = teamResult.claimedToCoord[i][j];
				outgoingSub.normalized = Common.UNINITIALIZED_INT;
				outgoingSub.normalizedToCoord = normalizedOutgoing;
				log.fine("Setting normalized outgoing of " + s.name + " to "
						+ outgoingSub.revieweeName + " to "
						+ normalizedOutgoing);
			}
		}
	}

	private void populateSubmissionsAndNames(
			HashMap<String, SubmissionData> list, TeamData team,
			StudentData student) {
		for (StudentData peer : team.students) {

			// get incoming submission from peer
			String key = peer.email + "->" + student.email;
			SubmissionData submissionFromPeer = list.get(key);
			// this workaround is to cater for missing submissions in
			// legacy data.
			if (submissionFromPeer == null) {
				log.warning("Cannot find submission for" + key);
				submissionFromPeer = createEmptySubmission(peer.email,
						student.email);
			} else {
				// use a copy to prevent accidental overwriting of data
				submissionFromPeer = submissionFromPeer.getCopy();
			}

			// set names in incoming submission
			submissionFromPeer.revieweeName = student.name;
			submissionFromPeer.reviewerName = peer.name;

			// add incoming submission
			student.result.incoming.add(submissionFromPeer);

			// get outgoing submission to peer
			key = student.email + "->" + peer.email;
			SubmissionData submissionToPeer = list.get(key);

			// this workaround is to cater for missing submissions in
			// legacy data.
			if (submissionToPeer == null) {
				log.warning("Cannot find submission for" + key);
				submissionToPeer = createEmptySubmission(student.email,
						peer.email);
			} else {
				// use a copy to prevent accidental overwriting of data
				submissionToPeer = submissionToPeer.getCopy();
			}

			// set names in outgoing submission
			submissionToPeer.reviewerName = student.name;
			submissionToPeer.revieweeName = peer.name;

			// add outgoing submission
			student.result.outgoing.add(submissionToPeer);

		}
	}

	private SubmissionData createEmptySubmission(String reviewer,
			String reviewee) {
		SubmissionData s;
		s = new SubmissionData();
		s.reviewer = reviewer;
		s.reviewee = reviewee;
		s.points = Common.UNINITIALIZED_INT;
		s.justification = new Text("");
		s.p2pFeedback = new Text("");
		s.course = "";
		s.evaluation = "";
		return s;
	}

	protected SubmissionData getSubmission(String courseId,
			String evaluationName, String reviewerEmail, String revieweeEmail) {
		Submission submission = EvaluationsStorage.inst().getSubmission(courseId,
				evaluationName, reviewerEmail, revieweeEmail);
		return (submission == null ? null : new SubmissionData(submission));
	}

	private boolean isInEnrollList(StudentData student,
			ArrayList<StudentData> studentInfoList) {
		for (StudentData studentInfo : studentInfoList) {
			if (studentInfo.email.equalsIgnoreCase(student.email))
				return true;
		}
		return false;
	}

	private boolean isSameAsExistingStudent(StudentData student) {
		StudentData existingStudent = getStudent(student.course, student.email);
		if (existingStudent == null)
			return false;
		return student.isEnrollInfoSameAs(existingStudent);
	}

	private boolean isModificationToExistingStudent(StudentData student) {
		return getStudent(student.course, student.email) != null;
	}

}