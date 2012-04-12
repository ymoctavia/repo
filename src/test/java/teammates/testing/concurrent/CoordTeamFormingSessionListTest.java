package teammates.testing.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;

import teammates.testing.config.Config;
import teammates.testing.lib.BrowserInstance;
import teammates.testing.lib.BrowserInstancePool;
import teammates.testing.lib.SharedLib;
import teammates.testing.lib.TMAPI;
import teammates.testing.object.Scenario;
import teammates.testing.object.Student;

/*
 * author Kalpit
 */
public class CoordTeamFormingSessionListTest extends TestCase {
	static BrowserInstance bi;
	static Scenario scn = setupScenarioInstance("teamForming");
	static Scenario scn2 = setupScenarioInstance("teamForming");

	@BeforeClass
	public static void classSetup() throws Exception {
		System.out.println("========== CoordTeamFormingSessionListTest");
		bi = BrowserInstancePool.request();

		TMAPI.cleanupCourse(scn.course.courseId);
			
		TMAPI.createCourse(scn.course);
		TMAPI.enrollStudents(scn.course.courseId, scn.students);

		TMAPI.createCourse(scn2.course);		
		TMAPI.enrollStudents(scn2.course.courseId, scn2.students);
		TMAPI.createTeamFormingSession(scn.teamFormingSession);
		TMAPI.createTeamFormingSession(scn2.teamFormingSession);
		
		bi.coordinatorLogin(scn.coordinator.username, scn.coordinator.password);
	}

	@AfterClass
	public static void classTearDown() throws Exception {
		bi.logout();
		TMAPI.cleanupCourse(scn.course.courseId);
		TMAPI.cleanupCourse(scn2.course.courseId);
		
		BrowserInstancePool.release(bi);
		System.out.println("CoordTeamFormingSessionListTest ==========//");
	}

	@Test
	public void verifyAddTeamFormingSessionPageSuccessful() throws Exception {
		bi.gotoTeamForming();
		int count = bi.countTotalTeamFormingSessions();
		assertEquals(2, count);
		
		assertEquals(true, bi.isTextPresent(scn.course.courseId));
		assertEquals(true, bi.isTextPresent(scn2.course.courseId));
	}
	
	@Test
	public void coordTeamFormingSessionViewLog() throws Exception {
		bi.gotoTeamForming();
		bi.clickTeamFormingSessionViewLog(scn.course.courseId);
		
		String noLog = "There is no log currently available.";
		String actualLog = bi.getElementText
			(By.xpath(String.format("//div[@id='coordinatorEvaluationManagement']//form//table[@class='headerform']//tbody//tr")));
		assertEquals(noLog, actualLog);
	}
}