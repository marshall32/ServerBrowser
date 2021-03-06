package com.msc.serverbrowser.gui.controllers.implementations

import com.msc.serverbrowser.Client
import com.msc.serverbrowser.data.PastUsernames
import com.msc.serverbrowser.gui.controllers.interfaces.ViewController
import com.msc.serverbrowser.util.samp.GTAController
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.ContextMenu
import javafx.scene.control.ListView
import javafx.scene.control.MenuItem
import javafx.scene.control.SelectionMode
import javafx.scene.control.TextField
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent

/**
 * ViewController for the Username View.
 *
 * @author Marcel
 */
class UsernameController: ViewController {
    @FXML
    private lateinit var usernameTextField: TextField

    @FXML
    private lateinit var nameList: ListView<String>

    private val applyNameMenuItem = MenuItem(Client.getString("applyUsername"))
    private val removeNameMenuItem = MenuItem(Client.getString("removeUsernameSingular"))

    private val menu = ContextMenu(applyNameMenuItem, removeNameMenuItem)

    override fun initialize() {
        usernameTextField.textProperty().bindBidirectional(GTAController.usernameProperty)

        nameList.selectionModel.selectionMode = SelectionMode.MULTIPLE

        nameList.items = FXCollections.observableArrayList<String>(PastUsernames.pastUsernames)
    }

    @FXML
    private fun onUsernameClicked(event: MouseEvent) {

        menu.hide()

        if (event.button == MouseButton.SECONDARY) {

            val selectedItems = nameList.selectionModel.selectedItems

            /*
			 * Making an array copy of the list, because iterating over the ObservableList would
			 * lead to mistakes.
			 */
            val usernames = selectedItems.toTypedArray<String>()

            showContextMenuForMultipleItems(event.screenX, event.screenY, *usernames)
        }
    }

    private fun showContextMenuForMultipleItems(showAtX: Double, showAtY: Double, vararg names: String) {

        if (names.isEmpty()) {
            return
        }

        val singleUsername = names.size == 1
        applyNameMenuItem.isVisible = singleUsername
        applyNameMenuItem.setOnAction {
            usernameTextField.text = names[0]
            applyUsername()
        }

        removeNameMenuItem.text = if (singleUsername) Client.getString("removeUsernameSingular") else Client.getString("removeUsernamePlural")
        removeNameMenuItem.setOnAction {
            for (name in names) {
                PastUsernames.removePastUsername(name)
                nameList.items.remove(name)
            }
        }

        menu.show(nameList, showAtX, showAtY)
    }

    @FXML
    private fun applyUsername() {
        GTAController.applyUsername()
        nameList.items = FXCollections.observableArrayList<String>(PastUsernames.pastUsernames)
    }

    override fun onClose() {
        // Do nothing
    }
}
