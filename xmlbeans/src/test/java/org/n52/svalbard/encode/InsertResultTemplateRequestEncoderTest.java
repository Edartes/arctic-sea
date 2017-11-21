/*
 * Copyright 2016-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.svalbard.encode;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.n52.shetland.ogc.gml.CodeType;
import org.n52.shetland.ogc.gml.CodeWithAuthority;
import org.n52.shetland.ogc.om.OmConstants;
import org.n52.shetland.ogc.om.OmObservableProperty;
import org.n52.shetland.ogc.om.OmObservationConstellation;
import org.n52.shetland.ogc.om.features.SfConstants;
import org.n52.shetland.ogc.om.features.samplingFeatures.InvalidSridException;
import org.n52.shetland.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.shetland.ogc.sensorML.SensorML;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.SosResultEncoding;
import org.n52.shetland.ogc.sos.SosResultStructure;
import org.n52.shetland.ogc.sos.request.InsertResultTemplateRequest;
import org.n52.shetland.ogc.swe.SweDataRecord;
import org.n52.shetland.ogc.swe.SweField;
import org.n52.shetland.ogc.swe.encoding.SweTextEncoding;
import org.n52.shetland.ogc.swe.simpleType.SweTime;
import org.n52.shetland.util.JTSHelper;
import org.n52.svalbard.decode.exception.DecodingException;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.encode.exception.UnsupportedEncoderInputException;
import org.n52.svalbard.util.XmlHelper;

import com.vividsolutions.jts.io.ParseException;

import net.opengis.om.x20.OMObservationType;
import net.opengis.sampling.x20.SFSamplingFeatureDocument;
import net.opengis.sampling.x20.SFSamplingFeatureType;
import net.opengis.sos.x20.InsertResultTemplateDocument;
import net.opengis.sos.x20.InsertResultTemplateType;
import net.opengis.sos.x20.ResultTemplateType;
import net.opengis.sos.x20.ResultTemplateType.ObservationTemplate;
import net.opengis.swe.x20.AbstractDataComponentType;
import net.opengis.swe.x20.AbstractEncodingType;
import net.opengis.swe.x20.DataRecordType;
import net.opengis.swe.x20.DataRecordType.Field;
import net.opengis.swe.x20.TextEncodingType;
import net.opengis.swe.x20.TimeType;

public class InsertResultTemplateRequestEncoderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private String templateIdentifier = "test-template-identifier";

    private String offering = "test-offering";

    private OmObservationConstellation observationTemplate;

    private InsertResultTemplateRequestEncoder encoder;

    private InsertResultTemplateRequest request;

    private String procedureIdentifier = "test-procedure-identifier";

    private String observedProperty = "test-observed-property";

    private String featureIdentifier = "test-feature-identifier";

    private String featureName = "test-feature-name";

    private String tokenSeparator = "@";

    private String blockSeparator = ";";

    private String field1Definition = "test-field-1-definition";

    private String field1Name = "test_field_1_name";

    private String field1Uom = "test-field-1-uom";

    @Before
    public void setup() throws InvalidSridException, ParseException {
        SensorML procedure = new SensorML();
        procedure.setIdentifier(procedureIdentifier);

        SamplingFeature featureOfInterest = new SamplingFeature(new CodeWithAuthority(featureIdentifier));
        featureOfInterest.setIdentifier(featureIdentifier);
        featureOfInterest.setName(new CodeType(featureName));
        featureOfInterest.setFeatureType(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_POINT);
        featureOfInterest.setGeometry(JTSHelper.createGeometryFromWKT("POINT(30 10)", 4326));

        observationTemplate = new OmObservationConstellation();
        observationTemplate.addOffering(offering);
        observationTemplate.setObservationType(OmConstants.OBS_TYPE_MEASUREMENT);
        observationTemplate.setProcedure(procedure);
        observationTemplate.setObservableProperty(new OmObservableProperty(observedProperty));
        observationTemplate.setFeatureOfInterest(featureOfInterest);

        SweTextEncoding textEncoding = new SweTextEncoding();
        textEncoding.setBlockSeparator(blockSeparator);
        textEncoding.setTokenSeparator(tokenSeparator);

        SweDataRecord resultStructure = new SweDataRecord();
        SweTime sweTime = new SweTime();
        sweTime.setDefinition(field1Definition);
        sweTime.setUom(field1Uom);
        resultStructure.addField(new SweField(field1Name, sweTime));

        request = new InsertResultTemplateRequest(SosConstants.SOS,
                Sos2Constants.SERVICEVERSION,
                Sos2Constants.Operations.InsertResultTemplate.name());
        request.setResultEncoding(new SosResultEncoding(textEncoding));
        request.setResultStructure(new SosResultStructure(resultStructure));
        request.setIdentifier(templateIdentifier);
        request.setObservationTemplate(observationTemplate);

        Supplier<XmlOptions> xmlOptions = () -> new XmlOptions();

        encoder = new InsertResultTemplateRequestEncoder();
        encoder.setXmlOptions(xmlOptions);

        OmEncoderv20 omEncoder = new OmEncoderv20();
        omEncoder.setXmlOptions(xmlOptions);

        SamplingEncoderv20 samsEncoder = new SamplingEncoderv20();
        samsEncoder.setXmlOptions(xmlOptions);

        GmlEncoderv321 gml32Encoder = new GmlEncoderv321();
        gml32Encoder.setXmlOptions(xmlOptions);

        SweCommonEncoderv20 sweEncoderv20 = new SweCommonEncoderv20();
        sweEncoderv20.setXmlOptions(xmlOptions);

        EncoderRepository encoderRepository = new EncoderRepository();
        encoderRepository.setEncoders(Arrays.asList(encoder, omEncoder, samsEncoder, gml32Encoder, sweEncoderv20));
        encoderRepository.init();

        encoderRepository.getEncoders().stream()
            .forEach(e -> ((AbstractDelegatingEncoder<?,?>)e).setEncoderRepository(encoderRepository));
    }

    @Test
    public void shouldThrowExceptionOnNullInput() throws EncodingException {
        thrown.expect(UnsupportedEncoderInputException.class);
        thrown.expectMessage(Is.is("Encoder " +
                InsertResultTemplateRequestEncoder.class.getSimpleName() +
                " can not encode 'null'"));

        encoder.create(null);
    }

    @Test
    public void shouldThrowExceptionWhenObservationTemplateIsMissing() throws EncodingException {
        thrown.expect(UnsupportedEncoderInputException.class);
        thrown.expectMessage(Is.is("Encoder " +
                InsertResultTemplateRequestEncoder.class.getSimpleName() +
                " can not encode 'missing ObservationTemplate'"));

        encoder.create(new InsertResultTemplateRequest());
    }

    @Test
    public void shouldThrowExceptionWhenOfferingIsMissing() throws EncodingException {
        thrown.expect(UnsupportedEncoderInputException.class);
        thrown.expectMessage(Is.is("Encoder " +
                InsertResultTemplateRequestEncoder.class.getSimpleName() +
                " can not encode 'missing offering'"));

        request = new InsertResultTemplateRequest();
        request.setObservationTemplate(new OmObservationConstellation());
        encoder.create(request);
    }

    @Test
    public void shouldThrowExceptionWhenResultStructureIsMissing() throws EncodingException {
        thrown.expect(UnsupportedEncoderInputException.class);
        thrown.expectMessage(Is.is("Encoder " +
                InsertResultTemplateRequestEncoder.class.getSimpleName() +
                " can not encode 'missing resultStructure'"));

        request = new InsertResultTemplateRequest();
        request.setObservationTemplate(new OmObservationConstellation());
        OmObservationConstellation observationTemplate = new OmObservationConstellation();
        observationTemplate.addOffering(offering);
        request.setObservationTemplate(observationTemplate);
        encoder.create(request);
    }

    @Test
    public void shouldThrowExceptionWhenResultEncodingIsMissing() throws EncodingException {
        thrown.expect(UnsupportedEncoderInputException.class);
        thrown.expectMessage(Is.is("Encoder " +
                InsertResultTemplateRequestEncoder.class.getSimpleName() +
                " can not encode 'missing resultEncoding'"));

        request = new InsertResultTemplateRequest();
        request.setObservationTemplate(new OmObservationConstellation());
        request.setResultStructure(new SosResultStructure(new SweDataRecord()));
        OmObservationConstellation observationTemplate = new OmObservationConstellation();
        observationTemplate.addOffering(offering);
        request.setObservationTemplate(observationTemplate);
        encoder.create(request);
    }

    @Test
    public void shouldSetRequestDefaults() throws EncodingException {
        InsertResultTemplateType encodedRequest = ((InsertResultTemplateDocument) encoder.create(request))
                .getInsertResultTemplate();

        Assert.assertThat(encodedRequest.getService(), Is.is(SosConstants.SOS));
        Assert.assertThat(encodedRequest.getVersion(), Is.is(Sos2Constants.SERVICEVERSION));
    }

    @Test
    public void shouldSetOptionalIdentifier() throws EncodingException {
        ResultTemplateType template = ((InsertResultTemplateDocument) encoder.create(request))
                .getInsertResultTemplate().getProposedTemplate().getResultTemplate();

        Assert.assertThat(template.isSetIdentifier(), Is.is(true));
        Assert.assertThat(template.getIdentifier(), Is.is(templateIdentifier));
    }

    @Test
    public void shouldSetOffering() throws EncodingException {
        ResultTemplateType template = ((InsertResultTemplateDocument) encoder.create(request))
                .getInsertResultTemplate().getProposedTemplate().getResultTemplate();

        Assert.assertThat(template.getOffering(), Matchers.notNullValue());
        Assert.assertThat(template.getOffering(), Is.is(offering));
    }

    @Test
    public void shouldSetObservationTemplate() throws EncodingException {
        ResultTemplateType template = ((InsertResultTemplateDocument) encoder.create(request))
                .getInsertResultTemplate().getProposedTemplate().getResultTemplate();

        Assert.assertThat(template.getObservationTemplate(), Matchers.notNullValue());
    }

    @Test
    public void shouldEncodeObservationTemplate() throws EncodingException, XmlException, IOException {
        ResultTemplateType template = ((InsertResultTemplateDocument) encoder.create(request))
                .getInsertResultTemplate().getProposedTemplate().getResultTemplate();

        ObservationTemplate observationTemplate = template.getObservationTemplate();
        Assert.assertThat(observationTemplate, Matchers.notNullValue());
        Assert.assertThat(observationTemplate, Matchers.instanceOf(ObservationTemplate.class));

        OMObservationType omObservation = observationTemplate.getOMObservation();
        Assert.assertThat(omObservation, Matchers.instanceOf(OMObservationType.class));
        Assert.assertThat(omObservation.getType().getHref(), Is.is(OmConstants.OBS_TYPE_MEASUREMENT));
        Assert.assertThat(omObservation.getPhenomenonTime().isNil(), Is.is(false));
        Assert.assertThat(omObservation.getPhenomenonTime().isSetNilReason(), Is.is(true));
        Assert.assertThat(omObservation.getPhenomenonTime().getNilReason(), Is.is("template"));
        Assert.assertThat(omObservation.getResultTime().isNil(), Is.is(false));
        Assert.assertThat(omObservation.getResultTime().isSetNilReason(), Is.is(true));
        Assert.assertThat(omObservation.getResultTime().getNilReason(), Is.is("template"));
        Assert.assertThat(omObservation.getProcedure().isNil(), Is.is(false));
        Assert.assertThat(omObservation.getProcedure().getHref(), Is.is(procedureIdentifier));
        Assert.assertThat(omObservation.getObservedProperty().isNil(), Is.is(false));
        Assert.assertThat(omObservation.getObservedProperty().getHref(), Is.is(observedProperty));
        Assert.assertThat(omObservation.getFeatureOfInterest(), Matchers.notNullValue());
        XmlObject xmlObject = XmlObject.Factory.parse(omObservation.getFeatureOfInterest().newInputStream());
        Assert.assertThat(xmlObject, Matchers.instanceOf(SFSamplingFeatureDocument.class));
        SFSamplingFeatureType feature = ((SFSamplingFeatureDocument) xmlObject).getSFSamplingFeature();
        Assert.assertThat(feature.getIdentifier().getStringValue(), Is.is(featureIdentifier));
        Assert.assertThat(feature.getNameArray().length, Is.is(1));
        Assert.assertThat(feature.getNameArray(0).getStringValue(), Is.is(featureName));
    }

    @Test
    public void shouldEncodeResultEncoding() throws EncodingException, DecodingException {
        ResultTemplateType template = ((InsertResultTemplateDocument) encoder.create(request))
                .getInsertResultTemplate().getProposedTemplate().getResultTemplate();

        XmlHelper.validateDocument(template);

        Assert.assertThat(template.getResultEncoding(), Matchers.notNullValue());
        Assert.assertThat(template.getResultEncoding().getAbstractEncoding(), Matchers.notNullValue());
        AbstractEncodingType resultEncoding = template.getResultEncoding().getAbstractEncoding();
        Assert.assertThat(resultEncoding, Matchers.instanceOf(TextEncodingType.class));
        TextEncodingType xbTextEncoding = (TextEncodingType) resultEncoding;
        Assert.assertThat(xbTextEncoding.getBlockSeparator(), Is.is(blockSeparator));
        Assert.assertThat(xbTextEncoding.getTokenSeparator(), Is.is(tokenSeparator));
    }

    @Test
    public void shouldEncodeResultStructure() throws EncodingException {
        ResultTemplateType template = ((InsertResultTemplateDocument) encoder.create(request))
                .getInsertResultTemplate().getProposedTemplate().getResultTemplate();

        Assert.assertThat(template.getResultStructure(), Matchers.notNullValue());
        AbstractDataComponentType abstractDataComponent = template.getResultStructure().getAbstractDataComponent();
        Assert.assertThat(abstractDataComponent, Matchers.notNullValue());
        Assert.assertThat(abstractDataComponent, Matchers.instanceOf(DataRecordType.class));
        DataRecordType xbResultStructure = (DataRecordType) abstractDataComponent;
        Assert.assertThat(xbResultStructure.getFieldArray().length, Is.is(1));
        Assert.assertThat(xbResultStructure.getFieldArray(0), Matchers.instanceOf(Field.class));
        Assert.assertThat(xbResultStructure.getFieldArray(0).getName(), Is.is(field1Name));
        Assert.assertThat(xbResultStructure.getFieldArray(0).getAbstractDataComponent(),
                Matchers.instanceOf(TimeType.class));
        TimeType xbTime = (TimeType) xbResultStructure.getFieldArray(0).getAbstractDataComponent();
        Assert.assertThat(xbTime.getDefinition(), Is.is(field1Definition));
        Assert.assertThat(xbTime.getUom().getCode(), Is.is(field1Uom));
    }
}
