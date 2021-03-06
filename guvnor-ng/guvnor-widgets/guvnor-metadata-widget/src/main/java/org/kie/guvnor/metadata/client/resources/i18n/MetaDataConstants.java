package org.kie.guvnor.metadata.client.resources.i18n;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

/**
 *
 */
public interface MetaDataConstants extends
                           Messages {

    public static final MetaDataConstants INSTANCE = GWT.create( MetaDataConstants.class );

    String Title();

    String Metadata();

    String LastModified();

    String ModifiedByMetaData();

    String NoteMetaData();

    String CreatedOnMetaData();

    String CreatedByMetaData();

    String IsDisabledMetaData();

    String DisableTip();

    String FormatMetaData();

    String OtherMetaData();

    String AShortDescriptionOfTheSubjectMatter();

    String TypeMetaData();

    String TypeTip();

    String ExternalLinkMetaData();

    String ExternalLinkTip();

    String SourceMetaData();

    String SourceMetaDataTip();

    String VersionHistory();

    String SubjectMetaData();

    String CategoriesMetaData();

    String AssetCategoryEditorAddNewCategory();

    String AddANewCategory();

    String OK();

    String SelectCategoryToAdd();

    String RemoveThisCategory();

    String PleaseWait();

    String NoCategoriesCreatedYetTip();

    String Refresh();

    String NewItem();

    String Trash();

    String RuleDocHint();

    String Description();

    String documentationDefault();

    String Discussion();

    String AddADiscussionComment();

    String EraseAllComments();

    String EraseAllCommentsWarning();

    String Cancel();

    String smallCommentBy0On1Small( final String author,
                                    final Date date );

    String VersionHistory1();

    String NoHistory();

    String View();

    String property0ModifiedOn1By23( final String version,
                                     final String lastModifier,
                                     final String lastModifiedDate,
                                     final String lastModifiedComment );

}
