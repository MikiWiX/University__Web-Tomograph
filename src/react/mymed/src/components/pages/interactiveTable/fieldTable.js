import React from 'react';
//import axios from 'axios';

//import styles from '../../Main.module.css';
import './fieldTable.css';

class FieldTable extends React.Component {

    tableElems = [];
    controlElems = [];
    keyCounter = 0;

    addImg = '/static/images/icoNotes/add.png';
    removeImg = '/static/images/icoNotes/remove.png';
    arrowUpImg = '/static/images/icoNotes/arrowUp.png';
    arrowDnImg = '/static/images/icoNotes/arrowDn.png';
    arrowImg = '/static/images/icoNotes/arrow.png';

    constructor(props){
        super(props);

        this.tableElems = [];
        this.tableElems.push(this.addEditableRow('Jill', 'Smith'));
        this.tableElems.push(this.addEditableRow('Eve', 'Jackson'));

        for (let i=0; i<this.tableElems.length; i++){
            this.addControlRow();
        }
    }

    componentDidMount() {

    }

    addEditableRow(text1, text2){
        this.keyCounter = this.keyCounter + 1;
        return (
            <tr key={this.keyCounter}>
                {this.addEditableCell(text1)}
                {this.addEditableCell(text2)}
            </tr>
        )
    }

    addEditableCell(text){
        this.keyCounter = this.keyCounter + 1;
        return(
            <td key={this.keyCounter}>
                <span onClick={this.editField}>{text}</span>
                <textarea type='text' onBlur={this.closeEditor} placeholder='lorem' className='noDisplay' />
            </td>
        )
    }

    addControlRow = () => {
        this.keyCounter = this.keyCounter + 1;
        this.controlElems.push(
            <tr key={this.keyCounter}>
                {this.addControlField(this.removeImg, 'rmIcon icon', this.removeRow, 'remove')}
                {this.addControlField(this.arrowUpImg, 'icon', this.moveUp, 'move up')}
                {this.addControlField(this.arrowDnImg, 'icon', this.moveDn, 'move down')}
            </tr>
        );
    }

    addControlField(src, clsName, action, alt){
        return(
            <td>
                <img src={src} className={clsName} onClick={action} alt={alt} />
            </td>
        )
    }

    addRow = () => {
        this.tableElems.push(this.addEditableRow('ID', 'Value'));
        this.addControlRow();
        this.forceUpdate();
    }

    editField = event => {
        let span = event.target;
        let textarea = event.target.parentNode.lastElementChild;

        span.classList.add('noDisplay');
        textarea.value = span.textContent;
        textarea.classList.remove('noDisplay');
    }

    closeEditor = event => {
        let span = event.target.parentNode.firstElementChild;
        let textarea = event.target;

        textarea.classList.add('noDisplay');
        span.textContent = textarea.value;
        span.classList.remove('noDisplay');
    }

    removeRow = event => {
        let row = event.target.parentNode.parentNode;
        let id = Array.prototype.indexOf.call(row.parentNode.rows, row);
        this.tableElems.splice(id, 1);
        this.controlElems.splice(id, 1);
        this.forceUpdate();
    }

    moveUp = event => {
        let row = event.target.parentNode.parentNode;
        let id = Array.prototype.indexOf.call(row.parentNode.rows, row);
        if(id > 0){
            this.tableElems.splice(id-1, 2, this.tableElems[id], this.tableElems[id-1]);
        }
        this.forceUpdate();
    }

    moveDn = event => {
        let row = event.target.parentNode.parentNode;
        let id = Array.prototype.indexOf.call(row.parentNode.rows, row);
        if(id < this.tableElems.length-1){
            this.tableElems.splice(id, 2, this.tableElems[id+1], this.tableElems[id]);
        }
        this.forceUpdate();
    }

    render() {
        let table =
            <table id='editable'>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Value</th>
                    </tr>
                </thead>
                <tbody>
                    {this.tableElems}
                </tbody>
            </table>

        let controls =
            <table id='controls'>
                <thead>
                    <tr>
                        <th className='empty' />
                        <th className='empty' />
                        <th className='empty' />
                    </tr>
                </thead>
                <tbody>
                    {this.controlElems}
                </tbody>
            </table>

        let addRow =
            <div id='newRow'>
                <img src={this.addImg} className='addRow' onClick={this.addRow} alt="new..." />
            </div>

        return (
            <div id='tableParent'>
                {table}
                {controls}
                {addRow}
            </div>
        )
    }
}

export default FieldTable;