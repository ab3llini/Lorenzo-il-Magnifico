package netobject.action.immediate;

import netobject.action.Action;
import netobject.action.ActionType;
import netobject.action.SelectionType;

/*
 * @author  ab3llini
 * @since   01/06/17.
 */
public class ImmediateChoiceAction extends Action {

    private final ImmediateActionType immediateActionType;

    private final int selection;

    public ImmediateChoiceAction(ImmediateActionType immediateActionType, int selection, String sender) {

        super(ActionType.Immediate, sender);

        this.immediateActionType = immediateActionType;
        this.selection = selection;
    }

    public int getSelection() {
        return selection;
    }

    @Override
    public String getSender() {
        return super.getSender();
    }
}
