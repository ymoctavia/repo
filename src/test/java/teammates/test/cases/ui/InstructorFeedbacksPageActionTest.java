package teammates.test.cases.ui;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.datatransfer.StudentAttributes;
import teammates.common.util.Const;
import teammates.logic.core.CoursesLogic;
import teammates.logic.core.EvaluationsLogic;
import teammates.logic.core.FeedbackSessionsLogic;
import teammates.ui.controller.InstructorFeedbacksPageAction;
import teammates.ui.controller.InstructorFeedbacksPageData;
import teammates.ui.controller.ShowPageResult;

public class InstructorFeedbacksPageActionTest extends BaseActionTest {

    DataBundle dataBundle;
    
    String unregUserId;
    String instructorId;
    String otherInstructorId;
    String studentId;
    String adminUserId;

    
    @BeforeClass
    public static void classSetUp() throws Exception {
        printTestClassHeader();
        uri = Const.ActionURIs.INSTRUCTOR_FEEDBACKS_PAGE;
    }

    @BeforeMethod
    public void caseSetUp() throws Exception {
        dataBundle = getTypicalDataBundle();

        unregUserId = "unreg.user";
        
        InstructorAttributes instructor1OfCourse1 = dataBundle.instructors.get("instructor1OfCourse1");
        instructorId = instructor1OfCourse1.googleId;
        
        InstructorAttributes instructor1OfCourse2 = dataBundle.instructors.get("instructor1OfCourse2");
        otherInstructorId = instructor1OfCourse2.googleId;
        
        StudentAttributes student1InCourse1 = dataBundle.students.get("student1InCourse1");
        studentId = student1InCourse1.googleId;
        
        adminUserId = "admin.user";
        
        restoreTypicalDataInDatastore();
    }
    
    @Test
    public void testAccessControl() throws Exception{
        
        String[] submissionParams = new String[]{
                Const.ParamsNames.COURSE_ID, 
                dataBundle.instructors.get("instructor1OfCourse1").courseId
        };
        verifyOnlyInstructorsOfTheSameCourseCanAccess(submissionParams);
    }
    
    @Test
    public void testExecuteAndPostProcess() throws Exception{
        
        String[] submissionParams = new String[]{};
        
        InstructorAttributes instructor1ofCourse1 = dataBundle.instructors.get("instructor1OfCourse1");
        
        ______TS("Typical case, 2 courses");
        if (CoursesLogic.inst().isCoursePresent("new-course")){
            CoursesLogic.inst().deleteCourseCascade("new-course");
        }
        
        CoursesLogic.inst().createCourseAndInstructor(instructorId, "new-course", "New course");
        gaeSimulation.loginAsInstructor(instructorId);
        InstructorFeedbacksPageAction a = getAction(submissionParams);
        ShowPageResult r = (ShowPageResult)a.executeAndPostProcess();
        
        assertEquals(Const.ViewURIs.INSTRUCTOR_FEEDBACKS+"?error=false&user=idOfInstructor1OfCourse1", r.getDestinationWithParams());
        assertEquals(false, r.isError);
        assertEquals("", r.getStatusMessage());
        
        InstructorFeedbacksPageData pageData = (InstructorFeedbacksPageData) r.data;
        assertEquals(instructorId, pageData.account.googleId);
        assertEquals(2, pageData.courses.size());
        assertEquals(2, pageData.existingEvalSessions.size());
        assertEquals(6, pageData.existingFeedbackSessions.size());
        assertEquals(null, pageData.newFeedbackSession);
        assertEquals(null, pageData.courseIdForNewSession);
        
        String expectedLogMessage = "TEAMMATESLOG|||instructorFeedbacksPage|||instructorFeedbacksPage" +
                "|||true|||Instructor|||Instructor 1 of Course 1|||idOfInstructor1OfCourse1" +
                "|||instr1@course1.com|||Number of feedback sessions: 6|||/page/instructorFeedbacksPage";
        assertEquals(expectedLogMessage, a.getLogMessage());
        
        ______TS("no feedback, has eval");
        
        FeedbackSessionsLogic.inst().deleteFeedbackSessionsForCourse(instructor1ofCourse1.courseId);
        
        submissionParams = new String[]{Const.ParamsNames.COURSE_ID, instructor1ofCourse1.courseId};
        a = getAction(addUserIdToParams(instructorId, submissionParams));
        r = (ShowPageResult) a.executeAndPostProcess();
        
        assertEquals(Const.ViewURIs.INSTRUCTOR_FEEDBACKS+"?error=false&user=idOfInstructor1OfCourse1", r.getDestinationWithParams());
        assertEquals(false, r.isError);
        assertEquals("", r.getStatusMessage());
        
        pageData = (InstructorFeedbacksPageData) r.data;
        assertEquals(instructorId, pageData.account.googleId);
        assertEquals(2, pageData.courses.size());
        assertEquals(2, pageData.existingEvalSessions.size());
        assertEquals(0, pageData.existingFeedbackSessions.size());
        assertEquals(null, pageData.newFeedbackSession);
        assertEquals(instructor1ofCourse1.courseId, pageData.courseIdForNewSession);
        
        expectedLogMessage = "TEAMMATESLOG|||instructorFeedbacksPage|||instructorFeedbacksPage" +
                "|||true|||Instructor|||Instructor 1 of Course 1|||idOfInstructor1OfCourse1" +
                "|||instr1@course1.com|||Number of feedback sessions: 0|||/page/instructorFeedbacksPage";
        assertEquals(expectedLogMessage, a.getLogMessage());
        
        ______TS("0 sessions");
        
        EvaluationsLogic.inst().deleteEvaluationsForCourse(instructor1ofCourse1.courseId);
        
        submissionParams = new String[]{Const.ParamsNames.COURSE_ID, instructor1ofCourse1.courseId};
        a = getAction(addUserIdToParams(instructorId, submissionParams));
        r = (ShowPageResult) a.executeAndPostProcess();
        
        assertEquals(
                Const.ViewURIs.INSTRUCTOR_FEEDBACKS+"?message=You+have+not+created+any+sessions+yet." +
                        "+Use+the+form+above+to+create+a+session.&error=false&user=idOfInstructor1OfCourse1", 
                r.getDestinationWithParams());
        assertEquals(Const.StatusMessages.FEEDBACK_SESSION_EMPTY, 
                r.getStatusMessage());
        assertEquals(false, r.isError);
        
        pageData = (InstructorFeedbacksPageData) r.data;
        assertEquals(instructorId, pageData.account.googleId);
        assertEquals(2, pageData.courses.size());
        assertEquals(0, pageData.existingEvalSessions.size());
        assertEquals(0, pageData.existingFeedbackSessions.size());
        assertEquals(null, pageData.newFeedbackSession);
        assertEquals(instructor1ofCourse1.courseId, pageData.courseIdForNewSession);
        
        expectedLogMessage = "TEAMMATESLOG|||instructorFeedbacksPage|||instructorFeedbacksPage" +
                "|||true|||Instructor|||Instructor 1 of Course 1|||idOfInstructor1OfCourse1" +
                "|||instr1@course1.com|||Number of feedback sessions: 0|||/page/instructorFeedbacksPage";
        assertEquals(expectedLogMessage, a.getLogMessage());
        
        ______TS("Masquerade mode, 0 courses");
        
        gaeSimulation.loginAsAdmin(adminUserId);
        
        CoursesLogic.inst().deleteCourseCascade(instructor1ofCourse1.courseId);
        CoursesLogic.inst().deleteCourseCascade("new-course");
        
        submissionParams = new String[]{};
        a = getAction(addUserIdToParams(instructorId, submissionParams));
        r = (ShowPageResult) a.executeAndPostProcess();
        
        assertEquals(
                Const.ViewURIs.INSTRUCTOR_FEEDBACKS+"?message=You+have+not+created+any+courses+yet." +
                        "+Go+%3Ca+href%3D%22%2Fpage%2FinstructorCoursesPage%3Fuser%3DidOfInstructor1OfCourse1%22%3Ehere%3C%2Fa%3E+to+create+one.&error=false&user=idOfInstructor1OfCourse1", 
                r.getDestinationWithParams());
        assertEquals("You have not created any courses yet. Go <a href=\"/page/instructorCoursesPage?user=idOfInstructor1OfCourse1\">here</a> to create one.", r.getStatusMessage());
        assertEquals(false, r.isError);
        
        pageData = (InstructorFeedbacksPageData) r.data;
        assertEquals(instructorId, pageData.account.googleId);
        assertEquals(0, pageData.courses.size());
        assertEquals(0, pageData.existingEvalSessions.size());
        assertEquals(0, pageData.existingFeedbackSessions.size());
        assertEquals(null, pageData.newFeedbackSession);
        
        expectedLogMessage = "TEAMMATESLOG|||instructorFeedbacksPage|||instructorFeedbacksPage" +
                "|||true|||Instructor(M)|||Instructor 1 of Course 1|||idOfInstructor1OfCourse1" +
                "|||instr1@course1.com|||Number of feedback sessions: 0|||/page/instructorFeedbacksPage";
        assertEquals(expectedLogMessage, a.getLogMessage());
    }
    
    
    private InstructorFeedbacksPageAction getAction(String... params) throws Exception{
            return (InstructorFeedbacksPageAction) (gaeSimulation.getActionObject(uri, params));
    }

}
