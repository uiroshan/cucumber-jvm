package cucumber.runtime;

import gherkin.events.PickleEvent;
import gherkin.pickles.PickleTag;
import io.cucumber.tagexpressions.Expression;
import io.cucumber.tagexpressions.TagExpressionParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class TagPredicate implements PicklePredicate {
    private final List<Expression> expressions = new ArrayList<Expression>();

    public TagPredicate(List<String> tagExpressions) {
        if (tagExpressions == null) {
            return;
        }
        TagExpressionParser parser = new TagExpressionParser();
        for (String tagExpression : tagExpressions) {
            expressions.add(parser.parse(tagExpression));
        }
    }

    @Override
    public boolean apply(PickleEvent pickleEvent) {
        return apply(pickleEvent.pickle.getTags());
    }

    public boolean apply(Collection<PickleTag> pickleTags) {
        List<String> tags = new ArrayList<String>();
        for (PickleTag pickleTag : pickleTags) {
            tags.add(pickleTag.getName());
        }
        for (Expression expression : expressions) {
            if (!expression.evaluate(tags)) {
                return false;
            }
        }
        return true;
    }

}
