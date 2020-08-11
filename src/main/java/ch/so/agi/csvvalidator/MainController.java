package ch.so.agi.csvvalidator;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.views.View;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.interlis2.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ehi.basics.settings.Settings;
import ch.interlis.iom_j.csv.CsvReader;
import ch.interlis.ioxwkf.dbtools.IoxWkfConfig;

@Controller("/")
public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Get(uri="/ping", produces="text/plain")
    public String index() {
        return "ili2gpkg-web-service";
    }

    @Get("/")
    @View("index")
    public HttpStatus upload() {
        return HttpStatus.OK;
    }
    
    @Post(value = "/", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_OCTET_STREAM) 
    @View("upload")
    public HttpResponse<?> validate(CompletedFileUpload file, String modelName) {
        try {           
            if (file.getSize() == 0 || file.getFilename().trim().equalsIgnoreCase("") || file.getName() == null) {
                log.warn("No file was uploaded. Redirecting to starting page.");
                return HttpResponse.seeOther(URI.create("/"));
            }
            
            File tmpFolder = Files.createTempDirectory("csvvalidatorws-").toFile();
            if (!tmpFolder.exists()) {
                tmpFolder.mkdirs();
            }
            log.info("tmpFolder {}", tmpFolder.getAbsolutePath());

            Path uploadFilePath = Paths.get(tmpFolder.toString(), file.getFilename());
            byte[] bytes = file.getBytes();
            Files.write(uploadFilePath, bytes);
            String uploadFileName = uploadFilePath.toFile().getAbsolutePath();
            List<String> files = new ArrayList<String>();
            files.add(uploadFileName);
            log.info("uploadFileName {}", uploadFileName);
            
            // TODO expose to user
            boolean firstLineIsHeader = true;
            Character valueDelimiter = "\"".charAt(0);
            Character valueSeparator = ",".charAt(0);
            String encoding = "UTF-8";

            Settings settings = new Settings();

            settings.setValue(IoxWkfConfig.SETTING_FIRSTLINE,
                    firstLineIsHeader ? IoxWkfConfig.SETTING_FIRSTLINE_AS_HEADER : IoxWkfConfig.SETTING_FIRSTLINE_AS_VALUE);
            if (valueDelimiter != null) {
                settings.setValue(IoxWkfConfig.SETTING_VALUEDELIMITER, valueDelimiter.toString());
            }
            if (valueSeparator != null) {
                settings.setValue(IoxWkfConfig.SETTING_VALUESEPARATOR, valueSeparator.toString());
            }
            if (encoding != null) {
                settings.setValue(CsvReader.ENCODING, encoding);
            }

            settings.setValue(Validator.SETTING_MODELNAMES, modelName);

            settings.setValue(Validator.SETTING_LOGFILE, uploadFileName + ".log");

            boolean validationOk = new CsvValidatorImpl().validate(files.toArray(new String[files.size()]), settings);            
            String validationString = new String(Files.readAllBytes(Paths.get(new File(uploadFileName + ".log").getAbsolutePath())), StandardCharsets.UTF_8);

            return HttpResponse.ok().contentType(MediaType.TEXT_PLAIN).body(validationString);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return HttpResponse.badRequest("Something went wrong:\n\n" + e.getMessage());
        }
    }
}