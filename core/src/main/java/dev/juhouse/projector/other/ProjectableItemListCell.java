/*
 * Taked from https://gist.github.com/jewelsea/7821196
 */
package dev.juhouse.projector.other;

import java.util.ArrayList;
import java.util.List;

import dev.juhouse.projector.models.ProjectionListItem;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.input.*;
import lombok.Getter;


public class ProjectableItemListCell extends ListCell<ProjectionListItem> {
    public interface CellCallback<X> {
        void onDragDone(List<X> items);
    }

    @Getter
    private final CellCallback<ProjectionListItem> cellCallback;

    public ProjectableItemListCell(CellCallback<ProjectionListItem> cellCallback) {
        this.cellCallback = cellCallback;
        ListCell<ProjectionListItem> thisCell = this;

        setOnDragDetected(event -> {
            if (getItem() == null) {
                return;
            }

            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(getItem().getIdentity());

            dragboard.setDragView(ProjectableItemListCell.this.snapshot(null, null));
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
                ObservableList<ProjectionListItem> items = getListView().getItems();
                ProjectionListItem item = findItemByIdentity(db.getString());
                int draggedIdx = items.indexOf(item);
                int thisIdx = items.indexOf(getItem());

                items.set(draggedIdx, getItem());
                items.set(thisIdx, item);

                List<ProjectionListItem> itemscopy = new ArrayList<>(getListView().getItems());
                getListView().getItems().setAll(itemscopy);

                if (cellCallback != null) {
                    cellCallback.onDragDone(itemscopy);
                }

                success = true;
            }
            event.setDropCompleted(success);

            event.consume();
        });

        setOnDragDone(DragEvent::consume);
    }

    @Override
    public void updateItem(ProjectionListItem item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
        } else {
            setText(item.getTitle());
        }
    }

    private ProjectionListItem findItemByIdentity(String id) {
        return getListView()
                .getItems()
                .stream()
                .filter(i -> i.getIdentity().equals(id))
                .findAny()
                .get();
    }
}
