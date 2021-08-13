import React from 'react';
//import axios from 'axios';

import styles from '../Main.module.css';
import './Task1.css';
import FieldTable from './interactiveTable/fieldTable.js'

class Task1 extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
          isFetching: false,
          quality: -1,
          autoBrightness: false
        };
        this.ToBase64.bind(this);
        this.imageList = [];
    }

    componentDidMount() {
        let slider = document.getElementById("slider");
        this.resetImages(slider);
    }

    ToBase64 = function (u8) {
        return btoa(String.fromCharCode.apply(null, new Uint8Array(u8)));
    }

    FromBase64 = function (str) {
        return atob(str).split('').map(function (c) { return c.charCodeAt(0); });
    }

    submitHandle = event => {

        event.preventDefault();
        let data = new FormData();
        let eventElems = event.target.elements;

        let autoBrightness = eventElems.autoBrightness.checked;

        if(eventElems.picture.files.length === 0 ||
            eventElems.alphaStep.value === '' ||
            eventElems.count.value === '' ||
            eventElems.spread.value === '' ||
            (eventElems.brightness.value === '' && !autoBrightness)){

            alert("Uzupełnij wymagane pola formularza!")
            return;
        }

        data.append('alphaStep', eventElems.alphaStep.value);
        data.append('count', eventElems.count.value);
        data.append('spread', eventElems.spread.value);
        data.append('brightness', eventElems.brightness.value);
        data.append('filter', eventElems.filter.checked);
        data.append('dynamicResolution', !(eventElems.resolution.value === 'static'));
        data.append('picture', eventElems.picture.files[0]);
        data.append('autoBrightness', autoBrightness);
        data.append('noSlider', eventElems.noSlider.checked);

        //console.log(event.target.elements.picture.files[0]);
        //console.log(JSON.stringify(Object.fromEntries(data)));

        fetch('//localhost:8080/tomograph', {
            method: 'PUT',
            //headers: { 'Content-Type': 'application/json' },
            //headers: { 'Content-Type': 'multipart/form-data' },
            body: data

        })
        //.then(response => response.json())
        .then((result) => {
            result.formData()
            .then((formdata) => {
                this.reloadImages(formdata);
                this.setState({ quality: parseFloat(formdata.get("quality"), 10) })
            })

        })
        .catch(error => {
          console.error('Error:', error);
        });
    }

    reloadImages(formdata){
        document.getElementById("inputImage").src = "data:image/png;base64," + formdata.get('file1');
        document.getElementById("processImage").src = "data:image/png;base64," + formdata.get('file2');
        document.getElementById("outputImage").src = "data:image/png;base64," + formdata.get('file3');

        let slider = document.getElementById("slider");
        this.resetImages(slider);
        if(formdata.get("imgCount")>0) {
            // add and display first image
            this.imageList.push(formdata.get("img"+0));
            document.getElementById('sliderImage').src = "data:image/png;base64," + this.imageList[0];
            slider.classList.remove(styles.hidden);

            for(let i=1; i<formdata.get("imgCount"); i++){
                this.addImage(slider, formdata.get("img"+i));
            }
        }
    }

    addImage(slider, image){
        slider.max = parseInt(slider.max) + 1;
        this.imageList.push(image);
    }

    resetImages(slider){
        slider.classList.add(styles.hidden);
        slider.value = 0;
        slider.min = 0;
        slider.max = 0;
        this.imageList = [];
    }

    displayImage = event => {
        document.getElementById('sliderImage').src = "data:image/png;base64," + this.imageList[event.target.value];
    }

    fetchDicom = event => {

        let data = new FormData();
        data.append('picture', document.getElementsByClassName('inputFormDiv')[0].firstElementChild.elements.picture.files[0]);

        //data.append('picture', event.target.elements.picture.files[0]);

        fetch('//localhost:8080/dicomGenerator', {
            method: 'PUT',
            //headers: { 'Content-Type': 'application/json' },
            //headers: { 'Content-Type': 'multipart/form-data' },
            body: data

        })
        //.then(response => response.json())
        .then((result) => {
            console.log(result);
            result.blob()
            .then((formdata) => {
                this.saveBlob(formdata, "newDicom.dcm");
            })


        })
        .catch(error => {
          console.error('Error:', error);
        });
    };

    saveBlob = function(blob, fileName) {
        var a = document.createElement("a");
        document.body.appendChild(a);
        a.style = "display: none";

        var url = window.URL.createObjectURL(blob);
        a.href = url;
        a.download = fileName;
        a.click();
        window.URL.revokeObjectURL(url);
    };

    updateBrightness = event => {
        this.setState({autoBrightness: event.target.checked});
    }

    render() {
        const fetchBlock = (this.state.isFetching) ?
            <div className={styles.loadingPane}> Fetching... </div>
            :null;

        const mainTask =
            <div className='taskMain'>
                <div className='inputFormDiv'>
                    <form onSubmit={this.submitHandle}>
                        <label htmlFor='picture'>Zamieść Obraz / Plik DICOM</label><br />
                        <input type="file" id="picture" name='picture' accept=".jpg, .jpeg, .png, .dcm" /><br />

                        <label htmlFor='alphaStep'>Krok ∆α układu emiter/detektor</label><br />
                        <input type='text' id='alphaStep' name='alphaStep' /><br />

                        <label htmlFor='count'>Liczba detektorów</label><br />
                        <input type='number' id='count' name='count' /><br />

                        <label htmlFor='spreadBox'>Rozwartość detektorów</label><br />

                        <input type='text' id='spread' name='spread' /><br />

                        <label htmlFor='autoBrightness'>Jasność wynikowa</label><br />
                        <input type='checkbox'  id='autoBrightness' name='autoBrightness' onClick={this.updateBrightness}/>Auto<br />
                        <input type='text' id='brightness' name='brightness' disabled={this.state.autoBrightness}/><br />

                        <input type='checkbox' defaultChecked="true" id='filter' name='filter' />
                        <label htmlFor='filter'>Filtrować?</label><br />

                        <label htmlFor='resolution'>Rozdzielczość:</label><br />
                        <label><input type='radio' value='dynamic' className='resolution' name='resolution' /> Dynamiczna (domyślna)</label><br />
                        <label><input type='radio' value='static' className='resolution' name='resolution' /> Wejściowa</label><br />

                        <input type='checkbox' id='noSlider' name='noSlider' />
                        <label htmlFor='noSlider'>Bez Slidera</label><br />

                        <input type='submit' value='Symuluj Tomograf' /><br />
                    </form>
                </div>
                <div className='dicomFormDiv'>
                    <span className={styles.none}><FieldTable /></span>
                    <br />
                    <input type='button' value='Wygeneruj DICOM' onClick={this.fetchDicom} />
                    <br /> <br />
                    <span id="quality" className={this.state.quality < 0 ? styles.hidden : undefined }> RMS: {this.state.quality} <br />(the lower the better)</span>
                    <br />
                </div>
                <div className='imageView0 sizedImage' id='inputImageDiv'>
                    Input:
                    <img id='inputImage' src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=" alt="input" />
                </div>
                <div className='imageView1 sizedImage' id='processImageDiv'>
                    Sinogram:
                    <img id='processImage' src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=" alt="process" />
                </div>
                <div className='imageView2 sizedImage' id='outputImageDiv'>
                    Output:
                    <img id='outputImage' src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=" alt="output" />
                </div>
                <div className='imageSlideShow sizedImage' id='imageSlideShowDiv'>
                    Slider:
                    <input className={styles.hidden+" slider"} type="range" min="0" max="100" defaultValue="50" id="slider" onChange={this.displayImage}/>
                    <img id='sliderImage' src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=" alt="sliderImage" />
                </div>
            </div>

        return (
            <main>
               {fetchBlock}
               {mainTask}
            </main>
        )}
}

export default Task1;