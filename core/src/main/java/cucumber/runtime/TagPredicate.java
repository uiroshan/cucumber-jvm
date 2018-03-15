package cucumber.runtime;

import gherkin.events.PickleEvent;
import gherkin.pickles.PickleTag;
import io.cucumber.tagexpressions.Expression;
import io.cucumber.tagexpressions.TagExpressionParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class TagPredicate implements PicklePredicate {
    private static final Expression TRUE = new Expression() {
        @Override
        public boolean evaluate(List<String> variables) {
            return true;
        }
    };
    private final Expression expression;

    public TagPredicate(String expression) {
        if(expression.trim().isEmpty()) {
            this.expression = TRUE;
        } else {
            TagExpressionParser parser = new TagExpressionParser();
            this.expression = parser.parse(expression);
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
        return expression.evaluate(tags);
    }

}
