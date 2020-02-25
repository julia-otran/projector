/*
 * Taked from https://gist.github.com/jewelsea/7821196
 */
package us.guihouse.projector.other;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import lombok.Getter;
import lombok.Setter;


public class DragSortListCell<X extends Identifiable> extends ListCell<X> {
    public interface DragDoneCallback<X> {
        void onDragDone(List<X> items);
    }

    @Getter
    private final DragDoneCallback<X> dragDoneCallback;

    @Override
    protected void finalize() throws Throwable {
        setOnDragDetected(null);
        setOnDragOver(null);
        setOnDragEntered(null);
        setOnDragExited(null);
        setOnDragDropped(null);
        setOnDragDone(null);
        super.finalize();
    }

    public DragSortListCell(DragDoneCallback<X> dragDoneCallback) {
        this.dragDoneCallback = dragDoneCallback;
        ListCell thisCell = this;

        setOnDragDetected(event -> {
            if (getItem() == null) {
                return;
            }

            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(getItem().getIdentity());

            dragboard.setDragView(DragSortListCell.this.snapshot(null, null));
            dragboard.setContent(content);

            event.consume();
        });

        setOnDragOver(event -> {
            if (event.getGestureSource() != thisCell &&
                   event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }

            event.consume();
        });

        setOnDragEntered(event -> {
            if (event.getGestureSource() != thisCell &&
                    event.getDragboard().hasString()) {
                setOpacity(0.3);
            }
        });

        setOnDragExited(event -> {
            if (event.getGestureSource() != thisCell &&
                    event.getDragboard().hasString()) {
                setOpacity(1);
            }
        });

        setOnDragDropped(event -> {
            if (getItem() == null) {
                return;
            }

            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasString()) {
                ObservableList<X> items = getListView().getItems();
                X item = findItemByIdentity(db.getString());
                int draggedIdx = items.indexOf(item);
                int thisIdx = items.indexOf(getItem());

                items.set(draggedIdx, getItem());
                items.set(thisIdx, item);

                List<X> itemscopy = new ArrayList<>(getListView().getItems());
                getListView().getItems().setAll(itemscopy);

                if (dragDoneCallback != null) {
                    dragDoneCallback.onDragDone(itemscopy);
                }

                success = true;
            }
            event.setDropCompleted(success);

            event.consume();
        });

        setOnDragDone(DragEvent::consume);
    }

    private X findItemByIdentity(String id) {
        return getListView()
                .getItems()
                .stream()
                .filter(i -> i.getIdentity().equals(id))
                .findAny()
                .get();
    }
}
