<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<%@ page import="java.util.Map"%>
<%@ page import="java.util.List"%>
<%@ page import="teammates.common.util.Const"%>
<%@ page import="teammates.common.datatransfer.FeedbackQuestionAttributes"%>
<%@ page import="teammates.common.datatransfer.FeedbackResponseAttributes"%>
<%@ page import="teammates.ui.controller.InstructorFeedbackResultsPageData"%>
<%@ page import="teammates.common.datatransfer.FeedbackAbstractQuestionDetails"%>
<%@ page import="teammates.common.datatransfer.FeedbackQuestionAttributes"%>
<%
    InstructorFeedbackResultsPageData data = (InstructorFeedbackResultsPageData)request.getAttribute("data");
%>
<!DOCTYPE html>
<html>
<head>
    <link rel="shortcut icon" href="/favicon.png">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>TEAMMATES - Feedback Session Results</title>
    <link rel="stylesheet" href="/stylesheets/common.css" type="text/css" media="screen">
    <link rel="stylesheet" href="/stylesheets/common-print.css" type="text/css" media="print">
    <link rel="stylesheet" href="/stylesheets/instructorFeedbacks.css" type="text/css" media="screen">
    
    <script type="text/javascript" src="/js/googleAnalytics.js"></script>
    <script type="text/javascript" src="/js/jquery-minified.js"></script>
    <script type="text/javascript" src="/js/tooltip.js"></script>
    <script type="text/javascript" src="/js/AnchorPosition.js"></script>
    <script type="text/javascript" src="/js/common.js"></script>
    <script type="text/javascript" src="/js/additionalQuestionInfo.js"></script>
    <jsp:include page="../enableJS.jsp"></jsp:include>
</head>

<body>
    <div id="dhtmltooltip"></div>
    <div id="frameTop">
        <jsp:include page="<%=Const.ViewURIs.INSTRUCTOR_HEADER%>" />
    </div>

    <div id="frameBody">
        <div id="frameBodyWrapper">
            <div id="topOfPage"></div>
            <div id="headerOperation">
                <h1>Feedback Results - Instructor</h1>
            </div>            
            <jsp:include page="<%=Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_TOP%>" />
            <br>
            <%
                for (Map.Entry<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> responseEntries : data.bundle
                        .getQuestionResponseMap().entrySet()) {
            %>
            <div class="backgroundBlock">
                    <h2 class="color_white multiline" style="padding-left: 20px;">Question <%=responseEntries.getKey().questionNumber%>:<br><%=data.bundle.getQuestionText(responseEntries.getKey().getId())%><%
                        Map<String, FeedbackQuestionAttributes> questions = data.bundle.questions;
                        FeedbackQuestionAttributes question = questions.get(responseEntries.getKey().getId());
                        FeedbackAbstractQuestionDetails questionDetails = question.getQuestionDetails();
                        out.print(questionDetails.getQuestionAdditionalInfoHtml(question.questionNumber, ""));
                    %></h2>
                    <table class="dataTable">
                        <tr>
                            <th class="leftalign color_white bold">
                                <input class="buttonSortNone" type="button" id="button_sortgiver" 
                                    onclick="toggleSort(this,1)">From:</th>
                            <th class="leftalign color_white bold">
                                <input class="buttonSortAscending" type="button" id="button_sortrecipient"
                                    onclick="toggleSort(this,2)">To:</th>
                            <th class="leftalign color_white bold">
                                <input class="buttonSortNone" type="button" id="button_sortanswer"
                                    onclick="toggleSort(this,3)">Feedback:</th>
                        </tr>
                        <%
                            for(FeedbackResponseAttributes responseEntry: responseEntries.getValue()) {
                        %>
                        <tr>
                        <%
                            String giverName = data.bundle.getGiverNameForResponse(responseEntries.getKey(), responseEntry);
                            String giverTeamName = data.bundle.getTeamNameForEmail(responseEntry.giverEmail);
                            giverName = data.bundle.appendTeamNameToName(giverName, giverTeamName);

                            String recipientName = data.bundle.getRecipientNameForResponse(responseEntries.getKey(), responseEntry);
                            String recipientTeamName = data.bundle.getTeamNameForEmail(responseEntry.recipientEmail);
                            recipientName = data.bundle.appendTeamNameToName(recipientName, recipientTeamName);
                        %>
                            <td class="middlealign"><%=giverName%></td>
                            <td class="middlealign"><%=recipientName%></td>
                            <td class="multiline"><%=responseEntry.getResponseDetails().getAnswerHtml()%></td>
                        </tr>        
                        <%
                            }
                        %>    
                    </table>
                </div>
            <br>
            <%
                }
            %>
        </div>
    </div>

    <div id="frameBottom">
        <jsp:include page="<%=Const.ViewURIs.FOOTER%>" />
    </div>
</body>
</html>