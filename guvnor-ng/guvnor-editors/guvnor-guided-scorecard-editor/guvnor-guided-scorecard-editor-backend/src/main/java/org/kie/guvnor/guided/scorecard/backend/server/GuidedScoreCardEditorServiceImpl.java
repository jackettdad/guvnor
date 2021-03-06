/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.guvnor.guided.scorecard.backend.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.StringUtils;
import org.dmg.pmml.pmml_4_1.descr.Attribute;
import org.dmg.pmml.pmml_4_1.descr.Characteristic;
import org.dmg.pmml.pmml_4_1.descr.Characteristics;
import org.dmg.pmml.pmml_4_1.descr.Extension;
import org.dmg.pmml.pmml_4_1.descr.FIELDUSAGETYPE;
import org.dmg.pmml.pmml_4_1.descr.INVALIDVALUETREATMENTMETHOD;
import org.dmg.pmml.pmml_4_1.descr.MiningField;
import org.dmg.pmml.pmml_4_1.descr.MiningSchema;
import org.dmg.pmml.pmml_4_1.descr.Output;
import org.dmg.pmml.pmml_4_1.descr.PMML;
import org.dmg.pmml.pmml_4_1.descr.Scorecard;
import org.drools.core.util.ArrayUtils;
import org.drools.scorecards.ScorecardCompiler;
import org.drools.scorecards.parser.xls.XLSKeywords;
import org.drools.scorecards.pmml.PMMLExtensionNames;
import org.drools.scorecards.pmml.PMMLGenerator;
import org.drools.scorecards.pmml.ScorecardPMMLUtils;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.commons.io.IOService;
import org.kie.commons.java.nio.base.options.CommentedOption;
import org.kie.commons.java.nio.file.NoSuchFileException;
import org.kie.guvnor.commons.service.validation.model.BuilderResult;
import org.kie.guvnor.commons.service.validation.model.BuilderResultLine;
import org.kie.guvnor.commons.service.verification.model.AnalysisReport;
import org.kie.guvnor.datamodel.oracle.DataModelOracle;
import org.kie.guvnor.datamodel.service.DataModelService;
import org.kie.guvnor.guided.scorecard.backend.server.util.ScoreCardsXMLPersistence;
import org.kie.guvnor.guided.scorecard.model.ScoreCardModel;
import org.kie.guvnor.guided.scorecard.model.ScoreCardModelContent;
import org.kie.guvnor.guided.scorecard.service.GuidedScoreCardEditorService;
import org.kie.guvnor.services.config.ResourceConfigService;
import org.kie.guvnor.services.config.model.ResourceConfig;
import org.kie.guvnor.services.metadata.MetadataService;
import org.kie.guvnor.services.metadata.model.Metadata;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.security.Identity;

@Service
@ApplicationScoped
public class GuidedScoreCardEditorServiceImpl
        implements GuidedScoreCardEditorService {

    @Inject
    @Named("ioStrategy")
    private IOService ioService;

    @Inject
    private Paths paths;

    @Inject
    private MetadataService metadataService;

    @Inject
    private ResourceConfigService resourceConfigService;

    @Inject
    private DataModelService dataModelService;

    @Inject
    private Identity identity;

    private static final String RESOURCE_EXTENSION = "scgd";

    @Override
    public ScoreCardModelContent loadContent( final Path path ) {
        final ScoreCardModel model = loadModel( path );
        final DataModelOracle oracle = dataModelService.getDataModel( path );
        return new ScoreCardModelContent( model,
                                          oracle );
    }

    @Override
    public ScoreCardModel loadModel( final Path path ) {
        return ScoreCardsXMLPersistence.getInstance().unmarshall( ioService.readAllString( paths.convert( path ) ) );
    }

    @Override
    public void save( final Path path,
                      final ScoreCardModel model,
                      final String comment ) {
        save( path,
              model,
              null,
              null,
              comment,
              null );
    }

    @Override
    public void save( final Path path,
                      final ScoreCardModel model,
                      final String comment,
                      final Date when,
                      final String lastContributor ) {
        save( path,
              model,
              null,
              null,
              comment,
              when,
              lastContributor );
    }

    @Override
    public void save( final Path resource,
                      final ScoreCardModel model,
                      final ResourceConfig config,
                      final Metadata metadata,
                      final String comment ) {
        save( resource,
              model,
              config,
              metadata,
              comment,
              null );
    }

    @Override
    public void save( final Path resource,
                      final ScoreCardModel model,
                      final ResourceConfig config,
                      final Metadata metadata,
                      final String comment,
                      final Date when ) {
        save( resource,
              model,
              config,
              metadata,
              comment,
              when,
              identity.getName() );
    }

    @Override
    public void save( final Path resource,
                      final ScoreCardModel model,
                      final ResourceConfig config,
                      final Metadata metadata,
                      final String comment,
                      final Date when,
                      final String lastContributor ) {

        final org.kie.commons.java.nio.file.Path path = paths.convert( resource );

        Map<String, Object> attrs;

        try {
            attrs = ioService.readAttributes( path );
        } catch ( final NoSuchFileException ex ) {
            attrs = new HashMap<String, Object>();
        }

        if ( config != null ) {
            attrs = resourceConfigService.configAttrs( attrs,
                                                       config );
        }
        if ( metadata != null ) {
            attrs = metadataService.configAttrs( attrs,
                                                 metadata );
        }

        ioService.write( path,
                         ScoreCardsXMLPersistence.getInstance().marshal( model ),
                         attrs,
                         new CommentedOption( lastContributor,
                                              comment,
                                              null,
                                              when ) );
    }

    @Override
    public String toSource( final ScoreCardModel model ) {
        final BuilderResult result = validateScoreCard( model );
        if ( !result.hasLines() ) {
            return toDRL( model );
        }
        return toDRL( result );
    }

    @Override
    public BuilderResult validate( final Path path,
                                   final ScoreCardModel model ) {
        final BuilderResult result = validateScoreCard( model );
        return result;
    }

    @Override
    public boolean isValid( final Path path,
                            final ScoreCardModel model ) {
        return !validate( path,
                          model ).hasLines();
    }

    @Override
    public AnalysisReport verify( final Path path,
                                  final ScoreCardModel content ) {
        //TODO {porcelli} verify
        return new AnalysisReport();
    }

    public String toDRL( final ScoreCardModel model ) {
        final PMML pmml = createPMMLDocument( model );
        return ScorecardCompiler.convertToDRL( pmml,
                                               ScorecardCompiler.DrlType.EXTERNAL_OBJECT_MODEL );
    }

    private String toDRL( final BuilderResult result ) {
        final StringBuilder drl = new StringBuilder();
        for ( final BuilderResultLine msg : result.getLines() ) {
            drl.append( "//" ).append( msg.getMessage() ).append( "\n" );
        }
        return drl.toString();
    }

    private PMML createPMMLDocument( final ScoreCardModel model ) {
        final Scorecard pmmlScorecard = ScorecardPMMLUtils.createScorecard();
        final Output output = new Output();
        final Characteristics characteristics = new Characteristics();
        final MiningSchema miningSchema = new MiningSchema();

        Extension extension = new Extension();
        extension.setName( PMMLExtensionNames.SCORECARD_RESULTANT_SCORE_CLASS );
        extension.setValue( model.getFactName() );

        pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas().add( extension );

        extension = new Extension();
        extension.setName( PMMLExtensionNames.SCORECARD_IMPORTS );
        pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas().add( extension );
        List<String> imports = new ArrayList<String>();
        imports.add( model.getFactName() );
        StringBuilder importBuilder = new StringBuilder();
        importBuilder.append( model.getFactName() );

        for ( final org.kie.guvnor.guided.scorecard.model.Characteristic characteristic : model.getCharacteristics() ) {
            if ( !imports.contains( characteristic.getFact() ) ) {
                imports.add( characteristic.getFact() );
                importBuilder.append( "," ).append( characteristic.getFact() );
            }
        }
        imports.clear();
        extension.setValue( importBuilder.toString() );

        extension = new Extension();
        extension.setName( PMMLExtensionNames.SCORECARD_RESULTANT_SCORE_FIELD );
        extension.setValue( model.getFieldName() );
        pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas().add( extension );

        extension = new Extension();
        extension.setName( PMMLExtensionNames.SCORECARD_PACKAGE );
        extension.setValue( model.getPackageName() );
        pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas().add( extension );

        final String modelName = convertToJavaIdentifier( model.getName() );
        pmmlScorecard.setModelName( modelName );
        pmmlScorecard.setInitialScore( model.getInitialScore() );
        pmmlScorecard.setUseReasonCodes( model.isUseReasonCodes() );

        if ( model.isUseReasonCodes() ) {
            pmmlScorecard.setBaselineScore( model.getBaselineScore() );
            pmmlScorecard.setReasonCodeAlgorithm( model.getReasonCodesAlgorithm() );
        }

        for ( final org.kie.guvnor.guided.scorecard.model.Characteristic characteristic : model.getCharacteristics() ) {
            final Characteristic _characteristic = new Characteristic();
            characteristics.getCharacteristics().add( _characteristic );

            extension = new Extension();
            extension.setName( PMMLExtensionNames.CHARACTERTISTIC_EXTERNAL_CLASS );
            extension.setValue( characteristic.getFact() );
            _characteristic.getExtensions().add( extension );

            extension = new Extension();
            extension.setName( PMMLExtensionNames.CHARACTERTISTIC_DATATYPE );
            if ( "string".equalsIgnoreCase( characteristic.getDataType() ) ) {
                extension.setValue( XLSKeywords.DATATYPE_TEXT );
            } else if ( "int".equalsIgnoreCase( characteristic.getDataType() ) || "double".equalsIgnoreCase( characteristic.getDataType() ) ) {
                extension.setValue( XLSKeywords.DATATYPE_NUMBER );
            } else if ( "boolean".equalsIgnoreCase( characteristic.getDataType() ) ) {
                extension.setValue( XLSKeywords.DATATYPE_BOOLEAN );
            } else {
                System.out.println( ">>>> Found unknown data type :: " + characteristic.getDataType() );
            }
            _characteristic.getExtensions().add( extension );

            if ( model.isUseReasonCodes() ) {
                _characteristic.setBaselineScore( characteristic.getBaselineScore() );
                _characteristic.setReasonCode( characteristic.getReasonCode() );
            }
            _characteristic.setName( characteristic.getName() );

            final MiningField miningField = new MiningField();
            miningField.setName( characteristic.getField() );
            miningField.setUsageType( FIELDUSAGETYPE.ACTIVE );
            miningField.setInvalidValueTreatment( INVALIDVALUETREATMENTMETHOD.RETURN_INVALID );
            miningSchema.getMiningFields().add( miningField );

            extension = new Extension();
            extension.setName( PMMLExtensionNames.CHARACTERTISTIC_EXTERNAL_CLASS );
            extension.setValue( characteristic.getFact() );
            miningField.getExtensions().add( extension );

            final String[] numericOperators = new String[]{ "=", ">", "<", ">=", "<=" };
            for ( final org.kie.guvnor.guided.scorecard.model.Attribute attribute : characteristic.getAttributes() ) {
                final Attribute _attribute = new Attribute();
                _characteristic.getAttributes().add( _attribute );

                extension = new Extension();
                extension.setName( PMMLExtensionNames.CHARACTERTISTIC_FIELD );
                extension.setValue( characteristic.getField() );
                _attribute.getExtensions().add( extension );

                if ( model.isUseReasonCodes() ) {
                    _attribute.setReasonCode( attribute.getReasonCode() );
                }
                _attribute.setPartialScore( attribute.getPartialScore() );

                final String operator = attribute.getOperator();
                final String dataType = characteristic.getDataType();
                String predicateResolver;
                if ( "boolean".equalsIgnoreCase( dataType ) ) {
                    predicateResolver = operator.toUpperCase();
                } else if ( "String".equalsIgnoreCase( dataType ) ) {
                    if ( operator.contains( "=" ) ) {
                        predicateResolver = operator + attribute.getValue();
                    } else {
                        predicateResolver = attribute.getValue() + ",";
                    }
                } else {
                    if ( ArrayUtils.contains( numericOperators, operator ) ) {
                        predicateResolver = operator + " " + attribute.getValue();
                    } else {
                        predicateResolver = attribute.getValue().replace( ",", "-" );
                    }
                }
                extension = new Extension();
                extension.setName( "predicateResolver" );
                extension.setValue( predicateResolver );
                _attribute.getExtensions().add( extension );
            }
        }

        pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas().add( miningSchema );
        pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas().add( output );
        pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas().add( characteristics );
        return new PMMLGenerator().generateDocument( pmmlScorecard );
    }

    private String convertToJavaIdentifier( final String modelName ) {
        final StringBuilder sb = new StringBuilder();
        if ( !Character.isJavaIdentifierStart( modelName.charAt( 0 ) ) ) {
            sb.append( "_" );
        }
        for ( char c : modelName.toCharArray() ) {
            if ( !Character.isJavaIdentifierPart( c ) ) {
                sb.append( "_" );
            } else {
                sb.append( c );
            }
        }
        return sb.toString();
    }

    private BuilderResult validateScoreCard( final ScoreCardModel model ) {
        final BuilderResult builderResult = new BuilderResult();
        if ( StringUtils.isBlank( model.getFactName() ) ) {
            builderResult.addLine( createBuilderResultLine( "Fact Name is empty.",
                                                            "Setup Parameters" ) );
        }
        if ( StringUtils.isBlank( model.getFieldName() ) ) {
            builderResult.addLine( createBuilderResultLine( "Resultant Score Field is empty.",
                                                            "Setup Parameters" ) );
        }
        if ( model.getCharacteristics().size() == 0 ) {
            builderResult.addLine( createBuilderResultLine( "No Characteristics Found.",
                                                            "Characteristics" ) );
        }
        int ctr = 1;
        for ( final org.kie.guvnor.guided.scorecard.model.Characteristic c : model.getCharacteristics() ) {
            String characteristicName = "Characteristic ('#" + ctr + "')";
            if ( StringUtils.isBlank( c.getName() ) ) {
                builderResult.addLine( createBuilderResultLine( "Name is empty.",
                                                                characteristicName ) );
            } else {
                characteristicName = "Characteristic ('" + c.getName() + "')";
            }
            if ( StringUtils.isBlank( c.getFact() ) ) {
                builderResult.addLine( createBuilderResultLine( "Fact is empty.",
                                                                characteristicName ) );
            }
            if ( StringUtils.isBlank( c.getField() ) ) {
                builderResult.addLine( createBuilderResultLine( "Characteristic Field is empty.",
                                                                characteristicName ) );
            } else if ( StringUtils.isBlank( c.getDataType() ) ) {
                builderResult.addLine( createBuilderResultLine( "Internal Error (missing datatype).",
                                                                characteristicName ) );
            }
            if ( c.getAttributes().size() == 0 ) {
                builderResult.addLine( createBuilderResultLine( "No Attributes Found.",
                                                                characteristicName ) );
            }
            if ( model.isUseReasonCodes() ) {
                if ( StringUtils.isBlank( model.getReasonCodeField() ) ) {
                    builderResult.addLine( createBuilderResultLine( "Resultant Reason Codes Field is empty.",
                                                                    characteristicName ) );
                }
                if ( !"none".equalsIgnoreCase( model.getReasonCodesAlgorithm() ) ) {
                    builderResult.addLine( createBuilderResultLine( "Baseline Score is not specified.",
                                                                    characteristicName ) );
                }
            }
            int attrCtr = 1;
            for ( final org.kie.guvnor.guided.scorecard.model.Attribute attribute : c.getAttributes() ) {
                final String attributeName = "Attribute ('#" + attrCtr + "')";
                if ( StringUtils.isBlank( attribute.getOperator() ) ) {
                    builderResult.addLine( createBuilderResultLine( "Attribute Operator is empty.",
                                                                    attributeName ) );
                }
                if ( StringUtils.isBlank( attribute.getValue() ) ) {
                    builderResult.addLine( createBuilderResultLine( "Attribute Value is empty.",
                                                                    attributeName ) );
                }
                if ( model.isUseReasonCodes() ) {
                    if ( StringUtils.isBlank( c.getReasonCode() ) ) {
                        if ( StringUtils.isBlank( attribute.getReasonCode() ) ) {
                            builderResult.addLine( createBuilderResultLine( "Reason Code must be set at either attribute or characteristic.",
                                                                            attributeName ) );
                        }
                    }
                }
                attrCtr++;
            }
            ctr++;
        }
        return builderResult;
    }

    private BuilderResultLine createBuilderResultLine( final String msg,
                                                       final String name ) {
        return new BuilderResultLine().setMessage( msg ).setResourceFormat( RESOURCE_EXTENSION ).setResourceName( name );
    }

}
