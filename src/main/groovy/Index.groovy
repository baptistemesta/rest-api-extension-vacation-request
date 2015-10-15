import bizdata.VacationRequest
import bizdata.VacationRequestDAOImpl
import groovy.json.JsonBuilder
import org.bonitasoft.console.common.server.page.PageContext
import org.bonitasoft.console.common.server.page.PageResourceProvider
import org.bonitasoft.console.common.server.page.RestApiController
import org.bonitasoft.console.common.server.page.RestApiResponse
import org.bonitasoft.console.common.server.page.RestApiResponseBuilder
import org.bonitasoft.console.common.server.page.RestApiUtil
import org.bonitasoft.engine.api.TenantAPIAccessor
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.business.data.SimpleBusinessDataReference
import org.bonitasoft.engine.session.APISession

import javax.servlet.http.HttpServletRequest

import static org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion.PRIORITY_DESC

class Index implements RestApiController {

    RestApiResponse doHandle(HttpServletRequest request, PageResourceProvider pageResourceProvider, PageContext pageContext, RestApiResponseBuilder apiResponseBuilder, RestApiUtil restApiUtil) {



        def session = pageContext.getApiSession()

        def processAPI = TenantAPIAccessor.getProcessAPI(session)
        def tasks = processAPI.getPendingHumanTaskInstances(session.getUserId(), 0, 100, PRIORITY_DESC)
        tasks.addAll(processAPI.getAssignedHumanTaskInstances(session.getUserId(), 0, 100, PRIORITY_DESC))

        def results = []
        tasks.each { task ->
            results.add([taskId: task.getId(), vacationRequest: getVacationRequest(task, session)])
        }
        JsonBuilder builder = new JsonBuilder(results)
        String table = builder.toPrettyString()
        return buildResponse(apiResponseBuilder, table)
    }

    def VacationRequest getVacationRequest(HumanTaskInstance task, APISession session) {
        def businessDataAPI = TenantAPIAccessor.getBusinessDataAPI(session)
        def businessDataReference = businessDataAPI.getProcessBusinessDataReference("vacationRequest", task.getParentProcessInstanceId()) as SimpleBusinessDataReference

        def vacationRequestDAOImpl = new VacationRequestDAOImpl(session)
        return vacationRequestDAOImpl.findByPersistenceId(businessDataReference.getStorageId())
    }

    protected RestApiResponse buildResponse(RestApiResponseBuilder apiResponseBuilder, Serializable result) {
        apiResponseBuilder.with {
            withResponse(result)
            build()
        }
    }

}

