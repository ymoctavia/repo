package teammates.test.cases.ui.browsertests;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.EvaluationAttributes;
import teammates.common.datatransfer.EvaluationAttributes.EvalStatus;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.util.Const;
import teammates.test.driver.BackDoor;
import teammates.test.driver.TestProperties;
import teammates.test.pageobjects.AppPage;
import teammates.test.pageobjects.Browser;
import teammates.test.pageobjects.BrowserPool;
import teammates.test.pageobjects.HomePage;
import teammates.test.pageobjects.InstructorHelpPage;
import teammates.test.pageobjects.InstructorHomePage;

/**
 * Tests Homepage and login page for instructors. 
 * SUT: {@link InstructorHomePage}.<br>
 * Uses a real account.
 * 
 */
public class InstructorHomePageUiTest extends BaseUiTestCase {
    private static DataBundle testData;
    private static Browser browser;
    private static InstructorHomePage homePage;
    
    private static EvaluationAttributes firstEval_OPEN;
    private static EvaluationAttributes secondEval_PUBLISHED;
    private static EvaluationAttributes thirdEval_CLOSED;
    private static EvaluationAttributes fourthEval_AWAITING;
    private static FeedbackSessionAttributes feedbackSession_AWAITING;
    private static FeedbackSessionAttributes feedbackSession_OPEN;
    private static FeedbackSessionAttributes feedbackSession_CLOSED;
    private static FeedbackSessionAttributes feedbackSession_PUBLISHED;

    @BeforeClass
    public static void classSetup() throws Exception {
        printTestClassHeader();
        testData = loadDataBundle("/InstructorHomePageUiTest.json");
        restoreTestDataOnServer(testData);
        browser = BrowserPool.getBrowser();
        
        firstEval_OPEN = testData.evaluations.get("First Eval");
        secondEval_PUBLISHED = testData.evaluations.get("Second Eval");
        thirdEval_CLOSED = testData.evaluations.get("Third Eval");
        fourthEval_AWAITING = testData.evaluations.get("Fourth Eval");
        
        feedbackSession_AWAITING = testData.feedbackSessions.get("Second Feedback Session");
        feedbackSession_OPEN = testData.feedbackSessions.get("First Feedback Session");
        feedbackSession_CLOSED = testData.feedbackSessions.get("Third Feedback Session");
        feedbackSession_PUBLISHED = testData.feedbackSessions.get("Fourth Feedback Session");
    }
    

    @Test
    public void allTests() throws Exception{
        testLogin();
        testContent();
        testShowFeedbackStatsLink();
        testHelpLink();
        testCourseLinks();
        testEvaluationLinks();
        testSearchAction();
        testSortAction();
        testRemindAction();
        testPublishUnpublishActions();
        testDeleteEvalAction();
        testArchiveCourseAction();
        testDeleteCourseAction();
    }
    
    private void testShowFeedbackStatsLink() {
        WebElement viewResponseLink = homePage.getViewResponseLink("CHomeUiT.CS2104", "First Feedback Session");
        
        String currentValidUrl = viewResponseLink.getAttribute("href");
        
        ______TS("test case: fail, fetch response rate of invalid url");
        homePage.setViewResponseLinkValue(viewResponseLink, "/invalid/url");
        viewResponseLink.click();
        homePage.verifyHtmlAjax("/InstructorHomeHTMLResponseRateFail.html");
        
        ______TS("test case: fail to fetch response rate again, check consistency of fail message");
        viewResponseLink = homePage.getViewResponseLink("CHomeUiT.CS2104", "First Feedback Session");
        viewResponseLink.click();
        homePage.verifyHtmlAjax("/InstructorHomeHTMLResponseRateFail.html");
        
        ______TS("test case: pass with valid url after multiple fails");
        viewResponseLink = homePage.getViewResponseLink("CHomeUiT.CS2104", "First Feedback Session");
        homePage.setViewResponseLinkValue(viewResponseLink, currentValidUrl);
        viewResponseLink.click();
        homePage.verifyHtmlAjax("/instructorHomeHTMLResponseRatePass.html");
    }


    public void testLogin(){
        
        ______TS("login");
        
        AppPage.logout(browser);
        homePage = HomePage.getNewInstance(browser)
                .clickInstructorLogin()
                .loginAsInstructor(
                        TestProperties.inst().TEST_INSTRUCTOR_ACCOUNT, 
                        TestProperties.inst().TEST_INSTRUCTOR_PASSWORD);
    }
    
    public void testContent(){
        
        ______TS("content: no courses");
        
        //this case is implicitly tested when testing for 'delete course' action
            
        ______TS("content: multiple courses");
        
        //already logged in
        homePage.verifyHtmlAjax("/InstructorHomeHTML.html");
        
        ______TS("content: new instructor");
        //TODO: to be implemented
        
    }
    
    public void testHelpLink() throws Exception{
        
        ______TS("link: help page");
        
        InstructorHelpPage helpPage = homePage.clickHelpLink();
        helpPage.closeCurrentWindowAndSwitchToParentWindow();
        
    }
    
    public void testCourseLinks(){
        //TODO: check Enroll, View, Edit, Add Evaluation links
    }
    
    public void testEvaluationLinks(){
        //TODO: check View results, Edit, Preview links
    }
    
    public void testRemindAction(){
        
        ______TS("remind action: AWAITING evaluation");
        
        homePage.verifyUnclickable(homePage.getRemindLink(fourthEval_AWAITING.courseId, fourthEval_AWAITING.name));
        
        ______TS("remind action: OPEN evaluation");
        
        homePage.clickAndCancel(homePage.getRemindLink(firstEval_OPEN.courseId, firstEval_OPEN.name));
        homePage.clickAndConfirm(homePage.getRemindLink(firstEval_OPEN.courseId, firstEval_OPEN.name))
            .verifyStatus(Const.StatusMessages.EVALUATION_REMINDERSSENT);
        
        //go back to previous page because 'send reminder' redirects to the 'Evaluations' page.
        homePage.goToPreviousPage(InstructorHomePage.class);
        
        ______TS("remind action: CLOSED evaluation");
        
        homePage.verifyUnclickable(homePage.getRemindLink(thirdEval_CLOSED.courseId, thirdEval_CLOSED.name));
        
        ______TS("remind action: PUBLISHED evaluation");
        
        homePage.verifyUnclickable(homePage.getRemindLink(secondEval_PUBLISHED.courseId, secondEval_PUBLISHED.name));
        
        ______TS("remind action: AWAITING feedback session");
        
        homePage.verifyUnclickable(homePage.getRemindLink(feedbackSession_AWAITING.courseId, feedbackSession_AWAITING.feedbackSessionName));
        
        ______TS("remind action: OPEN feedback session");
        
        homePage.clickAndCancel(homePage.getRemindLink(feedbackSession_OPEN.courseId, feedbackSession_OPEN.feedbackSessionName));
        homePage.clickAndConfirm(homePage.getRemindLink(feedbackSession_OPEN.courseId, feedbackSession_OPEN.feedbackSessionName))
            .verifyStatus(Const.StatusMessages.FEEDBACK_SESSION_REMINDERSSENT);
        
        //go back to previous page because 'send reminder' redirects to the 'Feedbacks' page.
        homePage.goToPreviousPage(InstructorHomePage.class);
        
        ______TS("remind action: CLOSED feedback session");
        
        homePage.verifyUnclickable(homePage.getRemindLink(feedbackSession_CLOSED.courseId, feedbackSession_CLOSED.feedbackSessionName));
        
        ______TS("remind action: PUBLISHED feedback session");
        
        homePage.verifyUnclickable(homePage.getRemindLink(feedbackSession_PUBLISHED.courseId, feedbackSession_PUBLISHED.feedbackSessionName));

    }

    public void testPublishUnpublishActions(){
        
        ______TS("publish action: AWAITING evaluation");
        
        homePage.verifyUnclickable(homePage.getPublishLink(fourthEval_AWAITING.courseId, fourthEval_AWAITING.name));
        
        ______TS("publish action: OPEN evaluation");
        
        homePage.verifyUnclickable(homePage.getPublishLink(firstEval_OPEN.courseId, firstEval_OPEN.name));
        
        ______TS("publish action: CLOSED evaluation");
        
        String courseId = thirdEval_CLOSED.courseId;
        String evalName = thirdEval_CLOSED.name;
        
        homePage.clickAndCancel(homePage.getPublishLink(courseId, evalName));
        assertEquals(EvalStatus.CLOSED, BackDoor.getEvaluation(courseId, evalName).getStatus());
        
        homePage.clickAndConfirm(homePage.getPublishLink(courseId, evalName))
            .verifyStatus(Const.StatusMessages.EVALUATION_PUBLISHED);
        assertEquals(EvalStatus.PUBLISHED, BackDoor.getEvaluation(courseId, evalName).getStatus());
        
        ______TS("unpublish action: PUBLISHED evaluation");
        
        homePage.clickAndCancel(homePage.getUnpublishLink(courseId, evalName));
        assertEquals(EvalStatus.PUBLISHED, BackDoor.getEvaluation(courseId, evalName).getStatus());
        
        homePage.clickAndConfirm(homePage.getUnpublishLink(courseId, evalName))
            .verifyStatus(Const.StatusMessages.EVALUATION_UNPUBLISHED);
        assertEquals(EvalStatus.CLOSED, BackDoor.getEvaluation(courseId, evalName).getStatus());
    }

    public void testDeleteEvalAction() throws Exception{
        
        ______TS("delete evaluation action");
        
        homePage.clickAndCancel(homePage.getDeleteEvalLink(firstEval_OPEN.courseId, firstEval_OPEN.name));
        assertNotNull(BackDoor.getEvaluation(firstEval_OPEN.courseId, firstEval_OPEN.name));
        
        homePage.clickAndConfirm(homePage.getDeleteEvalLink(firstEval_OPEN.courseId, firstEval_OPEN.name));
        assertTrue(BackDoor.isEvaluationNonExistent(firstEval_OPEN.courseId, firstEval_OPEN.name));
        homePage.verifyHtmlAjax("/instructorHomeEvalDeleteSuccessful.html");
        
    }
    
    public void testArchiveCourseAction() throws Exception {
        
        ______TS("archive course action");
        
        String courseId = testData.courses.get("CHomeUiT.CS1101").id;
        
        homePage.clickArchiveCourseLink(courseId);
        
        assertTrue(BackDoor.getCourse(courseId).isArchived);
        homePage.verifyHtmlAjax("/instructorHomeCourseArchiveSuccessful.html");
        
        ______TS("archive action failed");
        // only possible if someone else delete the course while the user is viewing the page
        // TODO: find out how to detect such an extreme case
        
    }

    public void testDeleteCourseAction() throws Exception{
        
        ______TS("delete course action");
        
        String courseId = testData.courses.get("CHomeUiT.CS2104").id;
        homePage.clickAndCancel(homePage.getDeleteCourseLink(courseId));
        assertNotNull(BackDoor.getCourse(courseId));
        
        homePage.clickAndConfirm(homePage.getDeleteCourseLink(courseId));
        assertTrue(BackDoor.isCourseNonExistent(courseId));
        homePage.verifyHtmlAjax("/instructorHomeCourseDeleteSuccessful.html");
        
        //delete the other course as well
        courseId = testData.courses.get("CHomeUiT.CS1101").id;
        BackDoor.deleteCourse(courseId);
        
        homePage.clickHomeTab();
        homePage.verifyHtml("/InstructorHomeHTMLEmpty.html");
        
    }
    
    public void testSearchAction() throws Exception{
        ______TS("search student");
        homePage.searchForStudent("Alice").verifySearchKey("Alice");
        homePage.verifyHtml("/instructorHomeSearchStudent.html");
        
        //go back to previous page because 'search' redirects to the 'StudentList' page.
        homePage.goToPreviousPage(InstructorHomePage.class);
    }
    
    public void testSortAction() throws Exception{
        ______TS("sort by id");
        homePage.clickSortByIdButton();
        homePage.verifyHtmlAjax("/InstructorHomeHTMLSortById.html");
        
        ______TS("sort by name");
        homePage.clickSortByNameButton();
        homePage.verifyHtmlAjax("/InstructorHomeHTMLSortByName.html");
        
        ______TS("sort by date");
        homePage.clickSortByDateButton();
        homePage.verifyHtmlAjax("/InstructorHomeHTMLSortByDate.html");
    }


    @AfterClass
    public static void classTearDown() throws Exception {
        BrowserPool.release(browser);
    }
}
