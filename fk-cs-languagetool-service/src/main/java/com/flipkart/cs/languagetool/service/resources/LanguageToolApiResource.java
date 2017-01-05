package com.flipkart.cs.languagetool.service.resources;

import com.flipkart.cs.languagetool.service.LanguageToolService;
import com.flipkart.cs.languagetool.service.exception.ApiException;
import com.flipkart.cs.languagetool.service.mappers.MapperRuleMatches;
import com.flipkart.cs.languagetool.service.models.dao.RegisteredDictionaryDao;
import com.flipkart.cs.languagetool.service.models.domain.RegisteredDictionary;
import com.flipkart.cs.languagetool.service.models.dtos.*;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import org.languagetool.JLanguageTool;
import org.languagetool.rules.RuleMatch;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by anmol.kapoor on 02/01/17.
 */
@Slf4j
@Path("/languageTool")
public class LanguageToolApiResource {
    private final Provider<JLanguageTool> jLanguageToolProvider;
    private final MapperRuleMatches mapperRuleMatches;
    private final LanguageToolService languageToolService;
    private final RegisteredDictionaryDao registeredDictionaryDao;
    private final HibernateBundle hibernateBundle;

    @Inject
    public LanguageToolApiResource(Provider<JLanguageTool> jLanguageToolProvider, MapperRuleMatches mapperRuleMatches, LanguageToolService languageToolService, RegisteredDictionaryDao registeredDictionaryDao, HibernateBundle hibernateBundle) {
        this.jLanguageToolProvider = jLanguageToolProvider;
        this.mapperRuleMatches = mapperRuleMatches;
        this.languageToolService = languageToolService;
        this.registeredDictionaryDao = registeredDictionaryDao;
        this.hibernateBundle = hibernateBundle;
    }

    @POST
    @Path("/check")
    @UnitOfWork
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CheckTextResponse checkTextWithLanguageTool(@Valid CheckTextRequest request) throws ApiException {
        RegisteredDictionary dictionary = validateAndGetDictionary();
        JLanguageTool jLanguageTool = jLanguageToolProvider.get();
        List<RuleMatch> ruleMatchList = new ArrayList<>();
        try {

            ruleMatchList = jLanguageTool.check(request.getText());
            log.info("Rules Matched : " + ruleMatchList.size());
        } catch (IOException e) {
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
        return mapperRuleMatches.toCheckTextResponse(ruleMatchList, new CheckTextResponse(), jLanguageTool.getLanguage(), request.getText());
    }

    private RegisteredDictionary validateAndGetDictionary() throws ApiException {
        String shortCode = RequestHeaders.get().getDictionary();
        Optional<RegisteredDictionary> possibleDictionary = registeredDictionaryDao.findById(shortCode);
        if (!possibleDictionary.isPresent()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Unable to find dictionary with shortcode : " + shortCode);

        } else {
            return possibleDictionary.get();
        }

    }

    @POST
    @Path("/requestPhrases")
    @UnitOfWork
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<PhraseActionResponse> requestToAddWord(@Valid PhraseRequest phraseRequest) throws ApiException {

        RegisteredDictionary dictionary = validateAndGetDictionary();
        /// cleaning phrase request
        PhraseRequest cleanedRequest = new PhraseRequest();
        cleanedRequest.setStatus(phraseRequest.getStatus());
        cleanedRequest.setPhrases(new HashSet<>());
        for (String s : phraseRequest.getPhrases()) {
            cleanedRequest.getPhrases().add(s.trim().toLowerCase());
        }
        List<PhraseActionResponse> phraseActionResponses = languageToolService.bulkRequestActionOnPhrases(cleanedRequest, dictionary);
        return phraseActionResponses;

    }

//    @POST
//    @Path("/bulkUpload/{status}")
//    @UnitOfWork
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    public Response bulkUpload(@PathParam("status") RequestStatus requestStatus, @FormDataParam("file") InputStream fileAsInputStream) throws ApiException {
//        RegisteredDictionary dictionary = validateAndGetDictionary();
//        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
//        headers.putSingle("Content-Disposition", "attachment; filename=\"" + "BulkUpload-" + requestStatus + "-Response-" + (DateTime.now().toString("dd MMMM hh:mm aa")) + ".csv" + "\"");
//
//        StringWriter writer = new StringWriter();
//        try {
//            IOUtils.copy(fileAsInputStream, writer, "UTF-8");
//        } catch (IOException e) {
//            String msg = "Unable to convert file into string. +" + e.getLocalizedMessage();
//            log.error(msg, e);
//            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR, msg, e);
//        }
//        String fileAsString = writer.toString();
//        Set<String> phrases = Sets.newHashSet(Splitter.on("\n").omitEmptyStrings().trimResults().split(fileAsString));
//
//        UnitOfWorkAwareStreamingOutput streamingOutput = new UnitOfWorkAwareProxyFactory(hibernateBundle)
//                .create(UnitOfWorkAwareStreamingOutput.class);
//        streamingOutput.setParameters(languageToolService, phrases, requestStatus, dictionary.getShortCode(), registeredDictionaryDao);
//
//
//        return Response.ok().entity(streamingOutput).replaceAll(headers).build();
//    }

}




















